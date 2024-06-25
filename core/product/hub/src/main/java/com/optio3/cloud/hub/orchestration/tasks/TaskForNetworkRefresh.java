/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import static com.optio3.asyncawait.CompileTime.await;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.cloud.client.gateway.model.GatewayOperationToken;
import com.optio3.cloud.client.gateway.proxy.GatewayDiscoveryApi;
import com.optio3.cloud.hub.logic.spooler.ResultStagingSpooler;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.BACnetDeviceRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceRecord;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.StreamHelperNextAction;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.config.ProtocolConfig;
import com.optio3.protocol.model.config.ProtocolConfigForBACnet;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;

public class TaskForNetworkRefresh extends BaseGatewayTask
{
    private static final int TimeoutForListObjectsInHours = 4;
    private static final int TimeoutForReadObjectsInHours = 24;
    private static final int TimeoutForRefresh            = TimeoutForListObjectsInHours + TimeoutForReadObjectsInHours;

    enum State
    {
        AssignNetworks,

        TriggerDiscovery,
        WaitForTriggerDiscovery,
        TriggerListObjects,
        ListObjects,
        WaitForListObjects,
        TriggerReadAllValues,
        ReadAllValues,
        WaitForReadAllValues,
        TriggerSamplingConfiguration
    }

    public static class Settings
    {
        public boolean forceDiscovery;
        public boolean forceListObjects;
        public boolean forceReadObjects;
        public boolean forceSamplingConfiguration;
        public boolean dontQueueIfAlreadyActive;
        public int     sleepOnStart;
    }

    public Settings settings;

    public List<RecordLocator<NetworkAssetRecord>> targetNetworks;
    public List<RecordLocator<DeviceRecord>>       targetDevices;
    public GatewayConfigStateMachine               enumHelper;

    //--//

    public static BackgroundActivityRecord alreadyUpdating(SessionHolder sessionHolder,
                                                           GatewayAssetRecord rec_gateway) throws
                                                                                           Exception
    {
        RecordLocator<GatewayAssetRecord> loc_gateway  = sessionHolder.createLocator(rec_gateway);
        BackgroundActivityRecord          previousTask = null;

        for (TypedRecordIdentity<BackgroundActivityRecord> ri : BackgroundActivityRecord.findHandlers(sessionHolder, false, true, TaskForNetworkRefresh.class, loc_gateway))
        {
            BackgroundActivityRecord rec_task = sessionHolder.fromIdentityOrNull(ri);
            if (rec_task != null)
            {
                if (previousTask == null || previousTask.getCreatedOn()
                                                        .isBefore(rec_task.getCreatedOn()))
                {
                    previousTask = rec_task;
                }
            }
        }

        return previousTask;
    }

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        Settings settings,
                                                        GatewayAssetRecord rec_gateway,
                                                        Consumer<TaskForNetworkRefresh> callback) throws
                                                                                                  Exception
    {
        BackgroundActivityRecord rec_previousTask = alreadyUpdating(sessionHolder, rec_gateway);
        if (rec_previousTask != null && settings.dontQueueIfAlreadyActive)
        {
            return null;
        }

        rec_gateway.putMetadata(GatewayAssetRecord.WellKnownMetadata.gatewayNeedsNetworkRefresh, true);
        rec_gateway.putMetadata(GatewayAssetRecord.WellKnownMetadata.gatewayNeedsSamplingRefresh, true);

        BackgroundActivityRecord rec_task = BaseGatewayTask.scheduleTask(sessionHolder, rec_gateway, 0, null, TaskForNetworkRefresh.class, (t) ->
        {
            t.initializeTimeout(TimeoutForRefresh, TimeUnit.HOURS);

            t.settings = settings;

            if (callback != null)
            {
                callback.accept(t);
            }
        });

        if (rec_previousTask != null)
        {
            rec_task.transitionToWaiting(rec_previousTask, null);
        }

        return rec_task;
    }

    //--//

    @Override
    public String getTitle()
    {
        return String.format("[%s] Refresh networks", name_gateway);
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return loc_gateway;
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true, autoRetry = true)
    public CompletableFuture<Void> state_AssignNetworks() throws
                                                          Exception
    {

        int sleepOnStart = settings.sleepOnStart;
        if (sleepOnStart > 0)
        {
            settings.sleepOnStart = 0;
            return rescheduleDelayed(sleepOnStart, TimeUnit.SECONDS);
        }

        GatewayDiscoveryApi proxy = await(getDiscoveryProxy());
        if (proxy == null)
        {
            // Gateway not responding...
            return rescheduleDelayed(1, TimeUnit.MINUTES);
        }

        List<GatewayNetwork> currentNetworks = withLocatorReadonly(loc_gateway, (sessionHolder, rec_gateway) ->
        {
            return rec_gateway.collectNetworkConfiguration(true);
        });

        Set<String> changedNetworks = Sets.newHashSet(await(proxy.assignNetworks(currentNetworks)));
        if (changedNetworks.isEmpty())
        {
            if (!settings.forceDiscovery && !settings.forceListObjects && !settings.forceReadObjects)
            {
                if (settings.forceSamplingConfiguration)
                {
                    return continueAtState(State.TriggerSamplingConfiguration);
                }

                // Everything is up-to-date, exit.
                resetFlag();

                return markAsCompleted();
            }
        }
        else
        {
            loggerInstance.info("[%s] Change in networks, triggering rediscovery...", name_gateway);
        }

        if (targetNetworks == null)
        {
            targetNetworks = Lists.newArrayList();

            for (GatewayNetwork currentNetwork : currentNetworks)
            {
                if (changedNetworks.contains(currentNetwork.sysId))
                {
                    targetNetworks.add(new RecordLocator<>(NetworkAssetRecord.class, currentNetwork.sysId));
                }
            }
        }

        for (RecordLocator<NetworkAssetRecord> targetNetwork : CollectionUtils.asEmptyCollectionIfNull(targetNetworks))
        {
            withLocatorReadonly(targetNetwork, (sessionHolder, rec_network) ->
            {
                RecordHelper<AssetRecord> helperAsset = sessionHolder.createHelper(AssetRecord.class);

                if (!rec_network.hasAnyChildrenOfType(helperAsset, DeviceRecord.class))
                {
                    settings.forceDiscovery = true;
                }
            });
        }

        return continueAtState(State.TriggerDiscovery);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_TriggerDiscovery() throws
                                                            Exception
    {
        GatewayDiscoveryApi proxy = await(getDiscoveryProxy());
        if (proxy == null)
        {
            // Gateway not responding...
            return rescheduleDelayed(1, TimeUnit.MINUTES);
        }

        LazyEntities lazy    = new LazyEntities();
        boolean      trigger = false;

        try (SessionHolder sessionHolder = getSessionProvider().newReadOnlySession())
        {
            for (RecordLocator<NetworkAssetRecord> loc_network : CollectionUtils.asEmptyCollectionIfNull(targetNetworks))
            {
                NetworkAssetRecord rec_network = sessionHolder.fromLocator(loc_network);

                for (ProtocolConfig protocolConfig : rec_network.getProtocolsConfiguration())
                {
                    // Reset the id for sampling configuration.
                    protocolConfig.samplingConfigurationId = null;

                    if (protocolConfig instanceof ProtocolConfigForBACnet)
                    {
                        // Only rediscover BACnet if forced.
                        if (settings.forceDiscovery)
                        {
                            lazy.ensureBACnetProtocolRequest(rec_network);
                            trigger = true;
                        }
                    }

                    if (protocolConfig instanceof ProtocolConfigForIpn)
                    {
                        lazy.ensureIpnProtocolRequest(rec_network);
                        trigger = true;
                    }
                }
            }
        }

        if (trigger)
        {
            // BUGBUG: discovery time should be configurable.
            final int broadcastIntervals = 5;
            final int rebroadcastCount   = 3;

            GatewayOperationToken token = await(proxy.triggerDiscovery(lazy.entities, broadcastIntervals, rebroadcastCount));
            prepareWaitOperation(10, TimeUnit.HOURS, token);

            return continueAtState(State.WaitForTriggerDiscovery);
        }

        return continueAtState(State.TriggerListObjects);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForTriggerDiscovery() throws
                                                                   Exception
    {
        OpResult op = await(waitForOperations(true, null));
        switch (op)
        {
            case Success:
                loggerInstance.info("[%s] Discovery completed", name_gateway);

                return continueAtState(State.TriggerListObjects, 1, TimeUnit.SECONDS); // Delay a bit, to let staged results to be detected.

            case Failure:
                // BUGBUG: capture reason why it failed.
                loggerInstance.info("[%s] Discovery failed!", name_gateway);
                return markAsFailed("Discovery failed");

            default:
                return AsyncRuntime.NullResult;
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_TriggerListObjects() throws
                                                              Exception
    {
        ResultStagingSpooler spooler = getService(ResultStagingSpooler.class);
        if (spooler.areThereAnyUnprocessedObjects())
        {
            return rescheduleDelayed(500, TimeUnit.MILLISECONDS);
        }

        loggerInstance.info("[%s] Started listing objects...", name_gateway);

        enumHelper = new GatewayConfigStateMachine();
        enumHelper.init(CollectionUtils.asEmptyCollectionIfNull(targetNetworks));

        return continueAtState(State.ListObjects);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_ListObjects() throws
                                                       Exception
    {
        GatewayDiscoveryApi proxy = await(getDiscoveryProxy());
        if (proxy == null)
        {
            // Gateway not responding...
            return rescheduleDelayed(1, TimeUnit.MINUTES);
        }

        int total   = 0;
        int batches = 0;

        while (!enumHelper.isDone())
        {
            LazyEntities lazy = new LazyEntities();

            enumHelper.advance(getSessionProvider(), true, new GatewayConfigStateMachine.Provider()
            {
                @Override
                public boolean shouldEnumerateNetwork(RecordHelper<AssetRecord> helper_asset,
                                                      NetworkAssetRecord rec_network) throws
                                                                                      Exception
                {
                    if (settings.forceListObjects)
                    {
                        if (CollectionUtils.isNotEmpty(targetDevices))
                        {
                            // We want to filter by device.
                            return true;
                        }

                        lazy.ensureBACnetProtocolRequest(rec_network);
                        lazy.ensureIpnProtocolRequest(rec_network);

                        return false;
                    }

                    // Always enumerate IPN objects.
                    lazy.ensureIpnProtocolRequest(rec_network);

                    if (!rec_network.hasAnyChildrenOfType(helper_asset, DeviceRecord.class))
                    {
                        // No devices, force listing.
                        lazy.ensureBACnetProtocolRequest(rec_network);
                    }

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
                    if (shouldTarget(rec_device))
                    {
                        BACnetDeviceRecord rec_device2 = Reflection.as(rec_device, BACnetDeviceRecord.class);
                        if (rec_device2 != null)
                        {
                            boolean include = false;

                            DeviceElementRecord rec_object = rec_device2.findDeviceObject(helper_asset.wrapFor(DeviceElementRecord.class));
                            if (rec_object == null)
                            {
                                include = true;
                            }
                            else if (!rec_object.hasContents())
                            {
                                include = true;
                            }
                            else if (settings.forceListObjects)
                            {
                                include = true;
                            }

                            if (include)
                            {
                                lazy.ensureDeviceRequest(rec_device2, null);
                            }
                        }
                    }

                    //--//

                    enumHelper.doneWithDevice();

                    return GatewayConfigStateMachine.NextAction.Exit;
                }

                @Override
                public GatewayConfigStateMachine.NextAction endNetworkEnumeration(RecordHelper<AssetRecord> helper_asset,
                                                                                  NetworkAssetRecord rec) throws
                                                                                                          Exception
                {
                    return GatewayConfigStateMachine.NextAction.Continue;
                }
            });

            if (lazy.queuedNetworks > 0)
            {
                loggerInstance.info("[%s] Starting batch of objects listing (%,d networks, %,d devices)...", name_gateway, lazy.queuedNetworks, lazy.queuedDevices);

                GatewayOperationToken token = await(proxy.listObjects(lazy.entities));
                prepareWaitOperation(TimeoutForListObjectsInHours, TimeUnit.HOURS, token);

                total += lazy.queuedEntities;
                batches++;
            }

            flushStateToDatabase();
        }

        loggerInstance.info("[%s] Started all batches (%,d) of objects listing (%,d entries)", name_gateway, batches, total);

        return continueAtState(State.WaitForListObjects);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForListObjects() throws
                                                              Exception
    {
        OpResult op = await(waitForOperations(true, null));
        switch (op)
        {
            case Success:
                loggerInstance.info("[%s] Completed listing all objects", name_gateway);

                return continueAtState(State.TriggerReadAllValues, 5, TimeUnit.SECONDS); // Delay a bit, to allow staged results to be detected.

            case Failure:
                // BUGBUG: capture reason why it failed.
                loggerInstance.info("[%s] Object listing failed!", name_gateway);
                return markAsFailed("Object listing failed");

            default:
                return AsyncRuntime.NullResult;
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_TriggerReadAllValues() throws
                                                                Exception
    {
        ResultStagingSpooler spooler = getService(ResultStagingSpooler.class);
        if (spooler.areThereAnyUnprocessedObjects())
        {
            return rescheduleDelayed(500, TimeUnit.MILLISECONDS);
        }

        loggerInstance.info("[%s] Started reading object values...", name_gateway);

        enumHelper = new GatewayConfigStateMachine();
        enumHelper.init(CollectionUtils.asEmptyCollectionIfNull(targetNetworks));

        return continueAtState(State.ReadAllValues);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_ReadAllValues() throws
                                                         Exception
    {
        GatewayDiscoveryApi proxy = await(getDiscoveryProxy());
        if (proxy == null)
        {
            // Gateway not responding...
            return rescheduleDelayed(1, TimeUnit.MINUTES);
        }

        int total   = 0;
        int batches = 0;

        while (!enumHelper.isDone())
        {
            LazyEntities lazy = new LazyEntities();

            enumHelper.advance(getSessionProvider(), true, new GatewayConfigStateMachine.Provider()
            {
                @Override
                public boolean shouldEnumerateNetwork(RecordHelper<AssetRecord> helper_asset,
                                                      NetworkAssetRecord rec_network) throws
                                                                                      Exception
                {
                    loggerInstance.debug("[%s] readAllValues: network %s/%s", name_gateway, rec_network.getSysId(), rec_network.getCidr());

                    if (settings.forceReadObjects)
                    {
                        lazy.ensureBACnetProtocolRequest(rec_network);
                        lazy.ensureIpnProtocolRequest(rec_network);

                        if (targetDevices != null)
                        {
                            // We want to filter by device.
                            return true;
                        }

                        return false;
                    }

                    // Always enumerate IPN objects.
                    lazy.ensureIpnProtocolRequest(rec_network);

                    return true;
                }

                @Override
                public GatewayConfigStateMachine.NextAction handleDevice(RecordHelper<AssetRecord> helper_asset,
                                                                         NetworkAssetRecord rec_network,
                                                                         DeviceRecord rec_device) throws
                                                                                                  Exception
                {
                    if (shouldTarget(rec_device))
                    {
                        BACnetDeviceRecord rec_device2 = Reflection.as(rec_device, BACnetDeviceRecord.class);
                        if (rec_device2 != null)
                        {
                            loggerInstance.debug("[%s] readAllValues: device %s", name_gateway, rec_device2.getIdentityDescriptor());

                            rec_device.enumerateChildrenNoNesting(helper_asset.wrapFor(DeviceElementRecord.class), -1, null, (rec_object) ->
                            {
                                boolean include = false;

                                if (!rec_object.hasContents())
                                {
                                    include = true;
                                }
                                else if (settings.forceReadObjects)
                                {
                                    include = true;
                                }

                                if (include)
                                {
                                    loggerInstance.debug("[%s] readAllValues: object %s", name_gateway, rec_object.getIdentifier());

                                    BACnetObjectIdentifier objId = new BACnetObjectIdentifier(rec_object.getIdentifier());
                                    if (!objId.object_type.isUnknown()) // Ignore objects of unknown type.
                                    {
                                        lazy.ensureDeviceRequest(rec_device2, rec_object);
                                    }
                                }

                                return StreamHelperNextAction.Continue_Evict;
                            });
                        }
                    }

                    //--//

                    enumHelper.doneWithDevice();

                    // Reschedule to persist the state.
                    return GatewayConfigStateMachine.NextAction.Exit;
                }

                @Override
                public GatewayConfigStateMachine.NextAction beginNetworkEnumeration(RecordHelper<AssetRecord> helper_asset,
                                                                                    NetworkAssetRecord rec_network)
                {
                    return GatewayConfigStateMachine.NextAction.Continue;
                }

                @Override
                public GatewayConfigStateMachine.NextAction endNetworkEnumeration(RecordHelper<AssetRecord> helper_asset,
                                                                                  NetworkAssetRecord rec) throws
                                                                                                          Exception
                {
                    loggerInstance.debug("[%s] readAllValues: done network %s/%s", name_gateway, rec.getSysId(), rec.getCidr());

                    // Reschedule to persist the state.
                    return GatewayConfigStateMachine.NextAction.Exit;
                }
            });

            if (lazy.queuedNetworks > 0)
            {
                loggerInstance.info("[%s] Starting batch of objects reads (%,d networks, %,d devices)...", name_gateway, lazy.queuedNetworks, lazy.queuedDevices);

                GatewayOperationToken token = await(proxy.readAllValues(lazy.entities));
                prepareWaitOperation(TimeoutForReadObjectsInHours, TimeUnit.HOURS, token);

                total += lazy.queuedEntities;
                batches++;
            }

            flushStateToDatabase();
        }

        loggerInstance.info("[%s] Started all batches (%,d) of objects reads (%,d entries)", name_gateway, batches, total);

        return continueAtState(State.WaitForReadAllValues);
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_WaitForReadAllValues() throws
                                                                Exception
    {
        OpResult op = await(waitForOperations(true, null));
        switch (op)
        {
            case Success:
                loggerInstance.info("[%s] Completed reading all objects", name_gateway);

                return continueAtState(State.TriggerSamplingConfiguration, 5, TimeUnit.SECONDS); // Delay a bit, to allow staged results to be detected.

            case Failure:
                // BUGBUG: capture reason why it failed.
                loggerInstance.info("[%s] Reading object values failed!", name_gateway);
                return markAsFailed("Reading object values failed");

            default:
                return AsyncRuntime.NullResult;
        }
    }

    @BackgroundActivityMethod(stateClass = State.class, autoRetry = true)
    public CompletableFuture<Void> state_TriggerSamplingConfiguration() throws
                                                                        Exception
    {
        ResultStagingSpooler spooler = getService(ResultStagingSpooler.class);
        if (spooler.areThereAnyUnprocessedObjects())
        {
            return rescheduleDelayed(500, TimeUnit.MILLISECONDS);
        }

        for (RecordLocator<NetworkAssetRecord> loc_network : CollectionUtils.asEmptyCollectionIfNull(targetNetworks))
        {
            withLocatorOrNull(loc_network, (sessionHolder, rec_network) ->
            {
                if (rec_network != null)
                {
                    TaskForSamplingConfiguration.scheduleTaskIfNotRunning(sessionHolder, rec_network);
                }
            });
        }

        resetFlag();

        return markAsCompleted();
    }

    private void resetFlag() throws
                             Exception
    {
        withLocatorOrNull(loc_gateway, (sessionHolder, rec_gateway) ->
        {
            if (rec_gateway != null)
            {
                rec_gateway.putMetadata(GatewayAssetRecord.WellKnownMetadata.gatewayNeedsNetworkRefresh, null);
            }
        });
    }

    private boolean shouldTarget(DeviceRecord rec_device)
    {
        if (targetDevices == null)
        {
            return true;
        }

        for (RecordLocator<DeviceRecord> targetDevice : targetDevices)
        {
            if (targetDevice.sameRecord(rec_device))
            {
                return true;
            }
        }

        return false;
    }
}
