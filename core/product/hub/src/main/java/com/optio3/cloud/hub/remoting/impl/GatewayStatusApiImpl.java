/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.remoting.impl;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.annotation.Optio3RemoteOrigin;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.client.gateway.model.GatewayFeature;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.cloud.client.gateway.model.GatewayStatus;
import com.optio3.cloud.client.gateway.model.LogEntry;
import com.optio3.cloud.client.gateway.proxy.GatewayStatusApi;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.orchestration.tasks.TaskForNetworkRefresh;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.ResultStagingRecord;
import com.optio3.cloud.messagebus.channel.RpcOrigin;
import com.optio3.cloud.messagebus.transport.StableIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.LogHolder;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.GatewayDescriptor;
import com.optio3.protocol.model.config.ProtocolConfig;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@Optio3RemotableEndpoint(itf = GatewayStatusApi.class)
public class GatewayStatusApiImpl implements GatewayStatusApi
{
    public static final Logger LoggerInstance = new Logger(GatewayStatusApi.class);

    @Inject
    private HubApplication m_app;

    @Optio3RemoteOrigin
    private RpcOrigin m_origin;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @Override
    public CompletableFuture<Void> checkin(GatewayStatus status) throws
                                                                 Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            InstanceConfiguration cfg = m_app.getServiceNonNull(InstanceConfiguration.class);

            String                              instanceId  = status.instanceId;
            InstanceConfiguration.InstanceState state       = cfg.resolveInstanceId(sessionHolder, instanceId);
            GatewayAssetRecord                  rec_gateway = state.rec_gateway;

            String rpcId = m_origin.getRpcId();
            if (!StringUtils.equals(rec_gateway.getRpcId(), rpcId))
            {
                LoggerInstance.debug("Gateway '%s' reconnected with RPC Id '%s'", instanceId, rpcId);
                rec_gateway.setRpcId(rpcId);
            }

            //--//

            RecordHelper<GatewayAssetRecord> helper_gateway = sessionHolder.createHelper(GatewayAssetRecord.class);

            rec_gateway.handleStatusUpdate(helper_gateway, status);

            String sysId = rec_gateway.getSysId();
            m_origin.setContext(sysId, instanceId);

            StableIdentity stableIdentity = m_origin.getIdentity(sysId);
            if (stableIdentity != null)
            {
                String name = rec_gateway.getName();
                if (StringUtils.isNotEmpty(name))
                {
                    stableIdentity.displayName = name + " - " + instanceId;
                }
                else
                {
                    stableIdentity.displayName = instanceId;
                }

                stableIdentity.rpcId = rpcId;
            }

            List<GatewayNetwork> networksExpected = rec_gateway.collectNetworkConfiguration(true);

            GatewayNetwork.Delta delta = new GatewayNetwork.Delta(status.networks, networksExpected);
            if (delta.hasChanged())
            {
                TaskForNetworkRefresh.Settings settings = new TaskForNetworkRefresh.Settings();

                TaskForNetworkRefresh.scheduleTask(sessionHolder, settings, rec_gateway, null);
            }
            else if (rec_gateway.getMetadata(GatewayAssetRecord.WellKnownMetadata.gatewayNeedsNetworkRefresh))
            {
                TaskForNetworkRefresh.Settings settings = new TaskForNetworkRefresh.Settings();
                settings.dontQueueIfAlreadyActive = true;

                TaskForNetworkRefresh.scheduleTask(sessionHolder, settings, rec_gateway, null);
            }
            else if (status.canSupport(GatewayFeature.SamplingConfigurationId))
            {
                boolean refresh = rec_gateway.getMetadata(GatewayAssetRecord.WellKnownMetadata.gatewayNeedsSamplingRefresh);

                for (GatewayNetwork gatewayNetwork : delta.same)
                {
                    GatewayNetwork expected = CollectionUtils.findFirst(networksExpected, (o) -> o.equals(gatewayNetwork));
                    GatewayNetwork got      = CollectionUtils.findFirst(status.networks, (o) -> o.equals(gatewayNetwork));

                    for (ProtocolConfig expectedConfig : expected.protocolsConfiguration)
                    {
                        ProtocolConfig gotConfig = CollectionUtils.findFirst(got.protocolsConfiguration, (o) -> o.equals(expectedConfig));
                        if (!StringUtils.equals(expectedConfig.samplingConfigurationId, gotConfig.samplingConfigurationId))
                        {
                            refresh = true;
                            break;
                        }
                    }
                }

                if (refresh)
                {
                    TaskForNetworkRefresh.Settings settings = new TaskForNetworkRefresh.Settings();
                    settings.forceSamplingConfiguration = true;
                    settings.dontQueueIfAlreadyActive   = true;

                    TaskForNetworkRefresh.scheduleTask(sessionHolder, settings, rec_gateway, (t) ->
                    {
                        t.targetNetworks = CollectionUtils.transformToList(rec_gateway.getBoundNetworks(), sessionHolder::createLocator);
                    });
                }
            }

            sessionHolder.commit();

            return wrapAsync(null);
        }
    }

    @Override
    public CompletableFuture<Void> publishResults(List<GatewayDiscoveryEntity> entities) throws
                                                                                         Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<GatewayAssetRecord> helper = sessionHolder.createHelper(GatewayAssetRecord.class);

            //
            // Walk through entities to inject Gateway's sysIds into the stream.
            //
            for (GatewayDiscoveryEntity en_gateway : GatewayDiscoveryEntity.filter(entities, GatewayDiscoveryEntitySelector.Gateway))
            {
                TypedRecordIdentity<GatewayAssetRecord> ri_gateway = GatewayAssetRecord.findByInstanceId(helper, en_gateway.selectorValue);
                if (ri_gateway != null)
                {
                    GatewayAssetRecord rec_gateway = sessionHolder.fromIdentity(ri_gateway);

                    GatewayDescriptor desc = new GatewayDescriptor();
                    desc.sysId               = rec_gateway.getSysId();
                    en_gateway.selectorValue = desc.sysId;

                    for (GatewayDiscoveryEntity en_protocol : en_gateway.filter(GatewayDiscoveryEntitySelector.Protocol))
                    {
                        for (GatewayDiscoveryEntity en_device : en_protocol.filter(GatewayDiscoveryEntitySelector.Perf_Device))
                        {
                            en_device.setSelectorValueAsObject(desc);
                        }
                    }
                }
            }

            if (LoggerInstance.isEnabled(Severity.DebugVerbose))
            {
                LoggerInstance.debugVerbose("New results:\n%s", ObjectMappers.prettyPrintAsJson(entities));
            }

            try
            {
                InstanceConfiguration instanceCfg = m_app.getServiceNonNull(InstanceConfiguration.class);
                await(instanceCfg.preprocessResults(entities));
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Failed to pre-process results, due to %s", t);
            }

            ResultStagingRecord.queue(sessionHolder.createHelper(ResultStagingRecord.class), entities);

            sessionHolder.commit();

            return wrapAsync(null);
        }
    }

    @Override
    public CompletableFuture<Void> publishLog(String instanceId,
                                              List<LogEntry> entries) throws
                                                                      Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<GatewayAssetRecord> helper = sessionHolder.createHelper(GatewayAssetRecord.class);

            TypedRecordIdentity<GatewayAssetRecord> ri_gateway = GatewayAssetRecord.findByInstanceId(helper, instanceId);
            if (ri_gateway != null)
            {
                RecordLocked<GatewayAssetRecord> lock_gateway = sessionHolder.fromIdentityWithLock(ri_gateway, 30, TimeUnit.SECONDS);

                try (var logHandler = GatewayAssetRecord.allocateLogHandler(lock_gateway))
                {
                    try (LogHolder log = logHandler.newLogHolder())
                    {
                        for (LogEntry entry : entries)
                        {
                            log.addLineSync(1, entry.timestamp, null, entry.thread, entry.selector, entry.level, entry.line);
                        }
                    }
                }
            }

            sessionHolder.commit();

            return wrapAsync(null);
        }
    }
}
