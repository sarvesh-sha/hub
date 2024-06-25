/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.client.gateway.model.GatewayOperationStatus;
import com.optio3.cloud.client.gateway.model.GatewayOperationToken;
import com.optio3.cloud.client.gateway.proxy.GatewayControlApi;
import com.optio3.cloud.client.gateway.proxy.GatewayDiscoveryApi;
import com.optio3.cloud.hub.orchestration.AbstractHubActivityHandler;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.BACnetDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.IpnDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.AsyncFunctionWithException;
import com.optio3.util.function.ConsumerWithException;
import org.apache.commons.lang3.StringUtils;

public abstract class BaseGatewayTask extends AbstractHubActivityHandler
{
    public enum OpResult
    {
        NotFound,
        Pending,
        Success,
        Failure
    }

    public static class LazyEntities
    {
        public List<GatewayDiscoveryEntity> entities = Lists.newArrayList();

        public int queuedEntities;
        public int queuedNetworks;
        public int queuedProtocols;
        public int queuedDevices;
        public int queuedElements;

        private String                 m_network_sysId;
        private GatewayDiscoveryEntity m_network;

        private GatewayDiscoveryEntity m_protocolBACnet;
        private String                 m_deviceBACnet_sysId;
        private GatewayDiscoveryEntity m_deviceBACnet;

        private GatewayDiscoveryEntity m_protocolIpn;
        private String                 m_deviceIpn_sysId;
        private GatewayDiscoveryEntity m_deviceIpn;

        private GatewayDiscoveryEntity m_protocolModbus;
        private String                 m_deviceModbus_sysId;
        private GatewayDiscoveryEntity m_deviceModbus;

        public GatewayDiscoveryEntity ensureNetworkRequest(NetworkAssetRecord rec_network)
        {
            String sysId = rec_network.getSysId();

            if (!StringUtils.equals(m_network_sysId, sysId))
            {
                queuedEntities++;
                queuedNetworks++;
                m_network_sysId = sysId;
                m_network = GatewayDiscoveryEntity.create(GatewayDiscoveryEntitySelector.Network, m_network_sysId);
                entities.add(m_network);

                m_protocolBACnet = null;
                m_protocolIpn = null;
                m_protocolModbus = null;
            }

            return m_network;
        }

        public GatewayDiscoveryEntity ensureBACnetProtocolRequest(NetworkAssetRecord rec_network)
        {
            GatewayDiscoveryEntity en = ensureNetworkRequest(rec_network);

            if (m_protocolBACnet == null)
            {
                queuedEntities++;
                queuedProtocols++;
                m_protocolBACnet = en.createAsRequest(GatewayDiscoveryEntitySelector.Protocol, GatewayDiscoveryEntity.Protocol_BACnet);
            }

            return m_protocolBACnet;
        }

        public GatewayDiscoveryEntity ensureIpnProtocolRequest(NetworkAssetRecord rec_network)
        {
            GatewayDiscoveryEntity en = ensureNetworkRequest(rec_network);

            if (m_protocolIpn == null)
            {
                queuedEntities++;
                queuedProtocols++;
                m_protocolIpn = en.createAsRequest(GatewayDiscoveryEntitySelector.Protocol, GatewayDiscoveryEntity.Protocol_Ipn);
            }

            return m_protocolIpn;
        }

        public GatewayDiscoveryEntity ensureDeviceRequest(BACnetDeviceRecord rec_device,
                                                          DeviceElementRecord rec_element)
        {
            String sysId = rec_device.getSysId();
            if (!StringUtils.equals(m_deviceBACnet_sysId, sysId))
            {
                GatewayDiscoveryEntity en = ensureBACnetProtocolRequest(rec_device.getParentAsset(NetworkAssetRecord.class));

                queuedEntities++;
                queuedDevices++;
                m_deviceBACnet_sysId = sysId;
                m_deviceBACnet = rec_device.createRequest(en);
            }

            if (rec_element != null)
            {
                queuedEntities++;
                queuedElements++;
                rec_element.createRequest(m_deviceBACnet, false);
            }

            return m_deviceBACnet;
        }

        public GatewayDiscoveryEntity ensureDeviceRequest(IpnDeviceRecord rec_device,
                                                          DeviceElementRecord rec_element)
        {
            String sysId = rec_device.getSysId();
            if (!StringUtils.equals(m_deviceIpn_sysId, sysId))
            {
                GatewayDiscoveryEntity en = ensureIpnProtocolRequest(rec_device.getParentAsset(NetworkAssetRecord.class));

                queuedEntities++;
                queuedDevices++;
                m_deviceIpn_sysId = sysId;
                m_deviceIpn = rec_device.createRequest(en);
            }

            if (rec_element != null)
            {
                queuedEntities++;
                queuedElements++;
                rec_element.createRequest(m_deviceIpn, false);
            }

            return m_deviceIpn;
        }
    }

    public static class PendingOperation
    {
        public GatewayOperationToken token;
        public ZonedDateTime         timeoutExpiration;
    }

    //--//

    public RecordLocator<GatewayAssetRecord> loc_gateway;
    public String                            name_gateway;

    public List<PendingOperation> pendingOperations = Lists.newArrayList();
    public int                    delayBeforeNextOperationCheck;

    //--//

    public static <T extends BaseGatewayTask> BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                                                    GatewayAssetRecord rec_gateway,
                                                                                    long delay,
                                                                                    ChronoUnit unit,
                                                                                    Class<T> handler,
                                                                                    ConsumerWithException<T> configure) throws
                                                                                                                        Exception
    {
        return scheduleActivity(sessionHolder, delay, unit, handler, (newHandler) ->
        {
            // Weird Java visibility rule...
            BaseGatewayTask downcast = newHandler;
            downcast.loc_gateway = sessionHolder.createLocator(rec_gateway);
            downcast.name_gateway = rec_gateway.getName();

            configure.accept(newHandler);
        });
    }

    //--//

    protected <T> CompletableFuture<T> getProxy(Class<T> clz) throws
                                                              Exception
    {
        var ci = withLocatorReadonlyOrNull(loc_gateway, (sessionHolder, rec) -> rec != null ? rec.extractConnectionInfo() : null);
        if (ci != null)
        {
            return ci.getProxyOrNull(app, clz, 100);
        }

        return AsyncRuntime.asNull();
    }

    protected GatewayAssetRecord getGatewayRecord(SessionHolder sessionHolder)
    {
        return sessionHolder.fromLocatorOrNull(loc_gateway);
    }

    protected CompletableFuture<GatewayDiscoveryApi> getDiscoveryProxy() throws
                                                                         Exception
    {
        return getProxy(GatewayDiscoveryApi.class);
    }

    protected CompletableFuture<GatewayControlApi> getControlProxy() throws
                                                                     Exception
    {
        return getProxy(GatewayControlApi.class);
    }

    //--//

    protected void prepareWaitOperation(int timeout,
                                        TimeUnit unit,
                                        GatewayOperationToken token)
    {
        PendingOperation po = new PendingOperation();
        po.timeoutExpiration = TimeUtils.future(timeout, unit);
        po.token = token;

        pendingOperations.add(po);
        delayBeforeNextOperationCheck = 100;
    }

    CompletableFuture<OpResult> waitForOperations(boolean exitOnMissingGateway,
                                                  AsyncFunctionWithException<GatewayOperationToken, Void> checkResults) throws
                                                                                                                        Exception
    {
        GatewayControlApi proxy = await(getControlProxy());
        if (proxy == null)
        {
            if (exitOnMissingGateway)
            {
                await(markAsCompleted());
            }
            return wrapAsync(OpResult.NotFound);
        }

        boolean failed = false;

        while (true)
        {
            boolean madeProgress = false;

            for (PendingOperation po : pendingOperations)
            {
                if (TimeUtils.isTimeoutExpired(po.timeoutExpiration))
                {
                    loggerInstance.error("Failed operation '%s' due to timeout", po.token.id);
                    failed = true;
                }
            }

            if (failed)
            {
                break;
            }

            if (!pendingOperations.isEmpty())
            {
                //
                // Instead of testing each operation and generating a lot of load on the gateway, we test only first one.
                // If it's completed, we move on to the next one, and so on.
                //
                PendingOperation po = pendingOperations.get(0);

                try
                {
                    GatewayOperationStatus status = await(proxy.checkOperation(po.token));
                    if (status != null)
                    {
                        switch (status)
                        {
                            case Completed:
                                if (checkResults != null)
                                {
                                    await(checkResults.apply(po.token));
                                }

                                proxy.cancelOperation(po.token); // Don't wait.
                                pendingOperations.remove(0);
                                madeProgress = true;
                                break;

                            case Failed:
                                failed = true;
                                break;
                        }
                    }
                }
                catch (Throwable t)
                {
                    loggerInstance.error("Failed while waiting for operation '%s' to complete: %s", po.token.id, t);
                }
            }

            if (!madeProgress)
            {
                break;
            }
        }

        if (failed)
        {
            for (PendingOperation po : pendingOperations)
            {
                proxy.cancelOperation(po.token); // Don't wait.
            }

            pendingOperations.clear();

            return wrapAsync(OpResult.Failure);
        }

        if (pendingOperations.isEmpty())
        {
            return wrapAsync(OpResult.Success);
        }

        delayBeforeNextOperationCheck = Math.min(60 * 1000, delayBeforeNextOperationCheck + 100);

        rescheduleDelayed(delayBeforeNextOperationCheck, TimeUnit.MILLISECONDS);
        return wrapAsync(OpResult.Pending);
    }
}