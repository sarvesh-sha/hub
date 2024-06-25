/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import static com.optio3.asyncawait.CompileTime.await;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;

public class TaskForPropertyUpdate extends BaseGatewayTask
{
    enum State
    {
        PrepareNetworks,
        WriteToDevices,
        WaitForWriteToDevices,
        CompleteUpdate
    }

    public RecordLocator<NetworkAssetRecord> loc_network;
    public String                            name_network;

    public List<RecordLocator<NetworkAssetRecord>> loc_networks = Lists.newArrayList();
    public GatewayConfigStateMachine               enumHelper;
    public ZonedDateTime                           threshold;
    public boolean                                 mustRerun;

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

        var lst = BackgroundActivityRecord.findHandlers(sessionHolder, false, true, TaskForPropertyUpdate.class, sessionHolder.createLocator(rec_network));

        BackgroundActivityRecord rec_activity = sessionHolder.fromIdentityOrNull(CollectionUtils.firstElement(lst));
        if (rec_activity == null)
        {
            rec_activity = BaseGatewayTask.scheduleTask(sessionHolder, rec_gateway, 200, ChronoUnit.MILLIS, TaskForPropertyUpdate.class, (t) ->
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
        return String.format("[%s] Update properties", name_gateway);
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

        loggerInstance.info("[%s] Writes properties for network %s...", name_gateway, name_network);

        LazyEntities lazy2 = withLocatorOrNull(loc_network, (sessionHolder, rec_targetNetwork) ->
        {
            if (rec_targetNetwork == null)
            {
                // The network got deleted, exit.
                return null;
            }

            if (rec_targetNetwork.getMetadata(DeviceElementRecord.WellKnownMetadata.elementDesiredStateNeeded) == null)
            {
                // No need to update.
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
            return markAsCompleted();
        }

        enumHelper = new GatewayConfigStateMachine();
        enumHelper.init(loc_networks);

        return continueAtState(State.WriteToDevices);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WriteToDevices() throws
                                                          Exception
    {
        GatewayDiscoveryApi proxy = await(getDiscoveryProxy());
        if (proxy == null)
        {
            // Gateway not responding...
            return rescheduleDelayed(1, TimeUnit.MINUTES);
        }

        if (threshold == null)
        {
            threshold = TimeUtils.now();
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
                    loggerInstance.info("[%s] Preparing property update on network %s - %s...", name_gateway, rec_network.getName(), rec_network.getCidr());
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
                    loggerInstance.debug("[%s] UpdateProperties: device '%s'", name_gateway, rec_device.getIdentityDescriptor());

                    // Handle the various protocols.
                    if (SessionHolder.isEntityOfClass(rec_device, BACnetDeviceRecord.class))
                    {
                        GatewayDiscoveryEntity en_protocol = lazy.ensureBACnetProtocolRequest(rec_network);

                        m_objects += rec_device.preparePropertyUpdate(helper_asset.wrapFor(DeviceElementRecord.class), en_protocol);
                    }

                    if (SessionHolder.isEntityOfClass(rec_device, IpnDeviceRecord.class))
                    {
                        GatewayDiscoveryEntity en_protocol = lazy.ensureIpnProtocolRequest(rec_network);

                        m_objects += rec_device.preparePropertyUpdate(helper_asset.wrapFor(DeviceElementRecord.class), en_protocol);
                    }

                    enumHelper.doneWithDevice();

                    if (++m_devices >= 20 || m_objects >= 50)
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
                    loggerInstance.debug("[%s] UpdateProperties: done network %s/%s", name_gateway, rec_network.getSysId(), rec_network.getCidr());

                    // Reschedule to persist the state.
                    return GatewayConfigStateMachine.NextAction.Exit;
                }
            });

            if (lazy.queuedEntities > 0)
            {
                GatewayOperationToken token = await(proxy.writeValues(lazy.entities));
                prepareWaitOperation(5, TimeUnit.MINUTES, token);

                lazy.entities.clear();

                return continueAtState(State.WaitForWriteToDevices);
            }

            return rescheduleDelayed(0, null);
        }

        enumHelper = new GatewayConfigStateMachine();
        enumHelper.init(loc_networks);

        return continueAtState(State.CompleteUpdate);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForWriteToDevices() throws
                                                                 Exception
    {
        OpResult op = await(waitForOperations(true, null));
        switch (op)
        {
            case Success:
                enumHelper.doneWithDevice();

                return continueAtState(State.WriteToDevices); // Restart with the next device.

            case Failure:
                // BUGBUG: capture reason why it failed.
                loggerInstance.info("[%s] Sampling Configuration Update failed!", name_gateway);

                return continueAtState(State.WriteToDevices, 10, TimeUnit.SECONDS); // Retry.

            default:
                return AsyncRuntime.NullResult;
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_CompleteUpdate() throws
                                                          Exception
    {
        if (!enumHelper.isDone())
        {
            enumHelper.advance(getSessionProvider(), false, new GatewayConfigStateMachine.Provider()
            {
                private int m_devices;
                private int m_objects;

                @Override
                public boolean shouldEnumerateNetwork(RecordHelper<AssetRecord> helper_asset,
                                                      NetworkAssetRecord rec_network) throws
                                                                                      Exception
                {
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
                    rec_device.putMetadata(DeviceElementRecord.WellKnownMetadata.elementDesiredStateNeeded, null);
                    loggerInstance.debug("[%s] UpdateProperties: device '%s'", name_gateway, rec_device.getIdentityDescriptor());

                    AtomicBoolean mustRerun = new AtomicBoolean();

                    // Handle the various protocols.
                    if (SessionHolder.isEntityOfClass(rec_device, BACnetDeviceRecord.class))
                    {
                        m_objects += rec_device.completePropertyUpdate(helper_asset.wrapFor(DeviceElementRecord.class), threshold, mustRerun);
                    }

                    if (SessionHolder.isEntityOfClass(rec_device, IpnDeviceRecord.class))
                    {
                        m_objects += rec_device.completePropertyUpdate(helper_asset.wrapFor(DeviceElementRecord.class), threshold, mustRerun);
                    }

                    TaskForPropertyUpdate.this.mustRerun |= mustRerun.get();

                    enumHelper.doneWithDevice();

                    if (++m_devices >= 20 || m_objects >= 50)
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
                    rec_network.putMetadata(DeviceElementRecord.WellKnownMetadata.elementDesiredStateNeeded, null);

                    // Reschedule to persist the state.
                    return GatewayConfigStateMachine.NextAction.Exit;
                }
            });

            return rescheduleDelayed(0, null);
        }

        if (mustRerun)
        {
            mustRerun = false;

            return continueAtState(State.PrepareNetworks, 10, TimeUnit.SECONDS); // Retry.
        }

        return markAsCompleted();
    }
}
