/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import static com.optio3.asyncawait.CompileTime.await;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayOperationToken;
import com.optio3.cloud.client.gateway.proxy.GatewayDiscoveryApi;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.BACnetDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.IpnDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.protocol.model.config.ProtocolConfig;
import com.optio3.util.CollectionUtils;
import com.optio3.util.IdGenerator;

public class TaskForSamplingConfiguration extends BaseGatewayTask
{
    enum State
    {
        PrepareNetworks,
        WaitForPrepareNetworks,
        ConfigureDevices,
        WaitForConfigureDevice,
        CompleteConfiguration,
        WaitForCompleteConfiguration,
    }

    public RecordLocator<NetworkAssetRecord> loc_network;
    public String                            name_network;

    public List<RecordLocator<NetworkAssetRecord>> loc_networks = Lists.newArrayList();
    public GatewayConfigStateMachine               enumHelper;

    public String samplingConfigurationId;

    //--//

    public static BackgroundActivityRecord scheduleTaskIfNotRunning(SessionHolder sessionHolder,
                                                                    NetworkAssetRecord rec_network) throws
                                                                                                    Exception
    {
        GatewayAssetRecord rec_gateway = rec_network.getBoundGateway();
        if (rec_gateway == null)
        {
            // No gateway, no point in trying to update the sampling configuration.
            return null;
        }

        rec_gateway.putMetadata(GatewayAssetRecord.WellKnownMetadata.gatewayNeedsSamplingRefresh, true);

        var lst = BackgroundActivityRecord.findHandlers(sessionHolder, false, true, TaskForSamplingConfiguration.class, sessionHolder.createLocator(rec_network));

        BackgroundActivityRecord rec_activity = sessionHolder.fromIdentityOrNull(CollectionUtils.firstElement(lst));
        if (rec_activity == null)
        {
            rec_activity = BaseGatewayTask.scheduleTask(sessionHolder, rec_gateway, 100, ChronoUnit.MILLIS, TaskForSamplingConfiguration.class, (t) ->
            {
                t.initializeTimeout(30, TimeUnit.MINUTES);

                t.loc_network  = sessionHolder.createLocator(rec_network);
                t.name_network = rec_network.getName();
            });
        }

        return rec_activity;
    }

    //--//

    @Override
    public String getTitle()
    {
        return String.format("[%s] Refresh sampling configuration", name_gateway);
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return loc_network;
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, autoRetry = true)
    public CompletableFuture<Void> state_PrepareNetworks() throws
                                                           Exception
    {
        GatewayDiscoveryApi proxy = await(getDiscoveryProxy());
        if (proxy == null)
        {
            // Gateway not responding...
            return rescheduleDelayed(1, TimeUnit.MINUTES);
        }

        loggerInstance.info("[%s] Reconfiguring sampling for network %s...", name_gateway, name_network);

        LazyEntities lazy2 = withLocatorOrNull(loc_network, (sessionHolder, rec_targetNetwork) ->
        {
            if (rec_targetNetwork == null)
            {
                // The network got deleted, exit.
                return null;
            }

            LazyEntities lazy = new LazyEntities();

            loc_networks.clear();
            loc_networks.add(loc_network);

            // Handle the various protocols.
            lazy.ensureBACnetProtocolRequest(rec_targetNetwork);
            lazy.ensureIpnProtocolRequest(rec_targetNetwork);

            return lazy;
        });

        if (lazy2 == null || lazy2.queuedEntities == 0)
        {
            resetFlag();

            return markAsCompleted();
        }

        GatewayOperationToken token = await(proxy.startSamplingConfiguration(lazy2.entities));
        prepareWaitOperation(1, TimeUnit.MINUTES, token);

        return continueAtState(State.WaitForPrepareNetworks);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForPrepareNetworks() throws
                                                                  Exception
    {
        OpResult op = await(waitForOperations(true, null));
        switch (op)
        {
            case Success:
                enumHelper = new GatewayConfigStateMachine();
                enumHelper.init(loc_networks);

                return continueAtState(State.ConfigureDevices);

            case Failure:
                // BUGBUG: capture reason why it failed.
                loggerInstance.info("[%s] Sampling Configuration failed!", name_gateway);

                return continueAtState(State.PrepareNetworks, 10, TimeUnit.SECONDS); // Retry.

            default:
                return AsyncRuntime.NullResult;
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_ConfigureDevices() throws
                                                            Exception
    {
        GatewayDiscoveryApi proxy = await(getDiscoveryProxy());
        if (proxy == null)
        {
            // Gateway not responding...
            return rescheduleDelayed(1, TimeUnit.MINUTES);
        }

        if (!enumHelper.isDone())
        {
            LazyEntities lazy = new LazyEntities();

            enumHelper.advance(getSessionProvider(), true, new GatewayConfigStateMachine.Provider()
            {
                private int m_devices;
                private int m_objects;

                @Override
                public boolean shouldEnumerateNetwork(RecordHelper<AssetRecord> helper_asset,
                                                      NetworkAssetRecord rec_network) throws
                                                                                      Exception
                {
                    loggerInstance.info("[%s] Preparing sampling on network %s - %s...", name_gateway, rec_network.getName(), rec_network.getCidr());
                    return true;
                }

                @Override
                public GatewayConfigStateMachine.NextAction beginNetworkEnumeration(RecordHelper<AssetRecord> helper_asset,
                                                                                    NetworkAssetRecord rec_network)
                {
                    return GatewayConfigStateMachine.NextAction.Continue;
                }

                @Override
                public GatewayConfigStateMachine.NextAction handleDevice(RecordHelper<AssetRecord> helper_asset,
                                                                         NetworkAssetRecord rec_network,
                                                                         DeviceRecord rec_device) throws
                                                                                                  Exception
                {
                    loggerInstance.debug("[%s] ConfigureSampling: device '%s'", name_gateway, rec_device.getIdentityDescriptor());

                    // Handle the various protocols.
                    if (SessionHolder.isEntityOfClass(rec_device, BACnetDeviceRecord.class))
                    {
                        GatewayDiscoveryEntity en_protocol = lazy.ensureBACnetProtocolRequest(rec_network);

                        m_objects += rec_device.prepareSamplingConfiguration(helper_asset.wrapFor(DeviceElementRecord.class), en_protocol);
                    }

                    if (SessionHolder.isEntityOfClass(rec_device, IpnDeviceRecord.class))
                    {
                        GatewayDiscoveryEntity en_protocol = lazy.ensureIpnProtocolRequest(rec_network);

                        m_objects += rec_device.prepareSamplingConfiguration(helper_asset.wrapFor(DeviceElementRecord.class), en_protocol);
                    }

                    enumHelper.doneWithDevice();

                    if (++m_devices >= 20 || m_objects >= 4000)
                    {
                        // Reschedule to persist the state.
                        return GatewayConfigStateMachine.NextAction.Exit;
                    }

                    return GatewayConfigStateMachine.NextAction.Continue;
                }

                @Override
                public GatewayConfigStateMachine.NextAction endNetworkEnumeration(RecordHelper<AssetRecord> helper_asset,
                                                                                  NetworkAssetRecord rec_network) throws
                                                                                                                  Exception
                {
                    loggerInstance.debug("[%s] ConfigureSampling: done network %s/%s", name_gateway, rec_network.getSysId(), rec_network.getCidr());

                    // Reschedule to persist the state.
                    return GatewayConfigStateMachine.NextAction.Exit;
                }
            });

            if (lazy.queuedEntities > 0)
            {
                GatewayOperationToken token = await(proxy.updateSamplingConfiguration(lazy.entities));
                prepareWaitOperation(5, TimeUnit.MINUTES, token);

                lazy.entities.clear();

                return continueAtState(State.WaitForConfigureDevice);
            }

            return rescheduleDelayed(0, null);
        }

        return continueAtState(State.CompleteConfiguration);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForConfigureDevice() throws
                                                                  Exception
    {
        OpResult op = await(waitForOperations(true, null));
        switch (op)
        {
            case Success:
                enumHelper.doneWithDevice();

                return continueAtState(State.ConfigureDevices); // Restart with the next device.

            case Failure:
                // BUGBUG: capture reason why it failed.
                loggerInstance.info("[%s] Sampling Configuration Update failed!", name_gateway);

                return continueAtState(State.ConfigureDevices, 10, TimeUnit.SECONDS); // Retry.

            default:
                return AsyncRuntime.NullResult;
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CompleteConfiguration() throws
                                                                 Exception
    {
        GatewayDiscoveryApi proxy = await(getDiscoveryProxy());
        if (proxy == null)
        {
            // Gateway not responding...
            return rescheduleDelayed(1, TimeUnit.MINUTES);
        }

        LazyEntities lazy = new LazyEntities();

        samplingConfigurationId = IdGenerator.newGuid();

        for (RecordLocator<NetworkAssetRecord> loc_network : loc_networks)
        {
            withLocatorReadonly(loc_network, (sessionHolder, rec_network) ->
            {
                // Handle the various protocols, settings the sampling ID.
                lazy.ensureBACnetProtocolRequest(rec_network).contents = samplingConfigurationId;
                lazy.ensureIpnProtocolRequest(rec_network).contents    = samplingConfigurationId;
            });
        }

        GatewayOperationToken token = await(proxy.completeSamplingConfiguration(lazy.entities));
        prepareWaitOperation(5, TimeUnit.MINUTES, token);

        return continueAtState(State.WaitForCompleteConfiguration);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForCompleteConfiguration() throws
                                                                        Exception
    {
        OpResult op = await(waitForOperations(true, null));
        switch (op)
        {
            case Success:
                enumHelper = null;

                loggerInstance.info("[%s] Completed sampling configuration for network %s!", name_gateway, name_network);

                for (RecordLocator<NetworkAssetRecord> loc_network : loc_networks)
                {
                    withLocator(loc_network, (sessionHolder, rec_network) ->
                    {
                        List<ProtocolConfig> list = rec_network.getProtocolsConfiguration();
                        for (ProtocolConfig protocolConfig : list)
                        {
                            protocolConfig.samplingConfigurationId = samplingConfigurationId;
                        }
                        rec_network.setProtocolsConfiguration(list);
                    });
                }

                resetFlag();

                return markAsCompleted();

            case Failure:
                // BUGBUG: capture reason why it failed.
                loggerInstance.info("[%s] Sampling Configuration commit failed!", name_gateway);

                return continueAtState(State.CompleteConfiguration, 10, TimeUnit.SECONDS); // Retry.

            default:
                return AsyncRuntime.NullResult;
        }
    }

    //--//

    private void resetFlag() throws
                             Exception
    {
        withLocatorOrNull(loc_gateway, (sessionHolder, rec_gateway) ->
        {
            if (rec_gateway != null)
            {
                rec_gateway.putMetadata(GatewayAssetRecord.WellKnownMetadata.gatewayNeedsSamplingRefresh, null);
            }
        });
    }
}
