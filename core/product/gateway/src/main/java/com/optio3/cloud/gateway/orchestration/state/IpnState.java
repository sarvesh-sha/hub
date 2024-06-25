/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.orchestration.state;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.asyncawait.AsyncDelay;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayOperationTracker;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayState;
import com.optio3.cloud.client.gateway.orchestration.state.SamplingConfig;
import com.optio3.cloud.client.gateway.orchestration.state.SamplingRequest;
import com.optio3.cloud.gateway.GatewayApplication;
import com.optio3.concurrency.AsyncMutex;
import com.optio3.lang.Unsigned8;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.ipn.IpnManager;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.BaseObjectModel;
import com.optio3.protocol.model.DeviceReachability;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.FieldTemporalResolution;
import com.optio3.protocol.model.TransportPerformanceCounters;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.protocol.model.ipn.IpnDeviceDescriptor;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.protocol.model.ipn.IpnObjectsState;
import com.optio3.protocol.model.ipn.objects.IpnLocation;
import com.optio3.protocol.model.ipn.objects.stealthpower.BaseStealthPowerModel;
import com.optio3.protocol.model.ipn.objects.stealthpower.StealthPower_FDNY;
import com.optio3.protocol.model.obdii.iso15765.BaseIso15765ObjectModel;
import com.optio3.protocol.model.obdii.iso15765.VehicleSpeed;
import com.optio3.protocol.stealthpower.StealthPowerManager;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.MonotonousTime;
import com.optio3.util.Resources;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.BiConsumerWithException;
import com.optio3.util.function.ConsumerWithException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

class IpnState extends CommonState
{
    public static class SamplingEntry
    {
        public int    period;
        public String deviceId;
        public String objId;
        public String prop;
    }

    public static class SamplingEntries
    {
        public final List<SamplingEntry> entries = Lists.newArrayList();
    }

    static final Logger LoggerInstance        = GatewayApplication.LoggerInstance.createSubLogger(IpnState.class);
    static final Logger LoggerInstanceSamples = LoggerInstance.createSubLogger(SamplingEntry.class);

    //--//

    private final AsyncMutex m_mutex = new AsyncMutex();

    private IpnManager m_manager;

    private final Set<String>                            m_newObjectsSeenWhileSampling = Sets.newHashSet();
    private       Multimap<String, String>               m_samplingTargets;
    private       SamplingConfig<String, String, String> m_samplingState;
    private       SamplingConfig<String, String, String> m_samplingStateNext;

    //--//

    IpnState(GatewayState gatewayState,
             GatewayNetwork configuration)
    {
        super(gatewayState, configuration);
    }

    @Override
    public void close() throws
                        Exception
    {
        stop();
    }

    //--//

    @Override
    CompletableFuture<Boolean> start() throws
                                       Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            //
            // Initialize the Ipn manager.
            //
            if (m_manager == null)
            {
                LoggerInstance.info("Starting IPN manager on network '%s'", m_configuration.name);

                ProtocolConfigForIpn cfg = m_configuration.getProtocolConfiguration(ProtocolConfigForIpn.class);
                if (cfg != null)
                {
                    IpnManager manager = new IpnManager(cfg)
                    {
                        private boolean m_vehicleActivityDetected;

                        private boolean m_obdActive;
                        private MonotonousTime m_obdTimeout;

                        private MonotonousTime m_stealthPowerReflashDelay;
                        private boolean m_stealthPowerActivityDetected;
                        private boolean m_stealthPowerOfferingFirmware;
                        private boolean m_stealthPowerOfferedFirmware;
                        private String m_stealthPowerLastAppVersion;

                        private MonotonousTime m_stealthpowerLastBootLoaderDetection;
                        private MonotonousTime m_stealthpowerForceUpgrade;
                        private byte m_stealthPowerBootloadVersion;
                        private byte m_stealthPowerHardwareVersion;
                        private byte m_stealthPowerHardwareRevision;

                        @Override
                        protected void notifyTransport(String port,
                                                       boolean opened,
                                                       boolean closed)
                        {
                        }

                        @Override
                        protected void streamSamples(IpnObjectModel obj) throws
                                                                         Exception
                        {
                            BaseStealthPowerModel obj_stealthpower = Reflection.as(obj, BaseStealthPowerModel.class);
                            if (obj_stealthpower != null)
                            {
                                m_stealthPowerActivityDetected = true;
                                m_stealthpowerForceUpgrade     = null;

                                obj_stealthpower.handleFirmwareUpgrade(this::handleFirmwareUpgrade);

                                {
                                    StealthPower_FDNY obj_typed = Reflection.as(obj, StealthPower_FDNY.class);
                                    if (obj_typed != null)
                                    {
                                        StealthPowerManager mgr = accessSubManager(StealthPowerManager.class);
                                        if (mgr != null)
                                        {
                                            if (m_vehicleActivityDetected)
                                            {
                                                mgr.sendVehicleMovingNotification();
                                                m_vehicleActivityDetected = false;
                                            }

                                            if (TimeUtils.isTimeoutExpired(m_obdTimeout))
                                            {
                                                m_obdTimeout = TimeUtils.computeTimeoutExpiration(30, TimeUnit.SECONDS);

                                                mgr.sendObdStatus(m_obdActive);
                                                m_obdActive = false;
                                            }
                                        }
                                    }
                                }
                            }

                            //
                            // Whenever the vehicle is moving, notify the StealthPower MCU, if present.
                            //
                            VehicleSpeed obj_speed = Reflection.as(obj, VehicleSpeed.class);
                            if (obj_speed != null && Unsigned8.unboxOrDefault(obj_speed.value, (byte) 0) > 1)
                            {
                                m_vehicleActivityDetected = true;
                            }

                            IpnLocation obj_gps = Reflection.as(obj, IpnLocation.class);
                            if (obj_gps != null && obj_gps.speed > 10)
                            {
                                m_vehicleActivityDetected = true;
                            }

                            if (obj instanceof BaseIso15765ObjectModel)
                            {
                                m_obdActive = true;
                            }
                        }

                        @Override
                        protected void notifySamples(IpnObjectModel obj,
                                                     String field)
                        {
                            IpnObjectModel.FieldState  fieldState      = obj.getFieldState(field);
                            double                     nowEpochSeconds = IpnObjectModel.FieldSample.now();
                            IpnObjectModel.FieldSample sample;

                            while ((sample = fieldState.pop(nowEpochSeconds)) != null)
                            {
                                queueSample(sample.timestampEpochSeconds, sample.temporalResolution, obj, field, sample.value, false, sample.flushOnChange);
                            }
                        }

                        @Override
                        protected byte[] detectedStealthPowerBootloader(byte bootloadVersion,
                                                                        byte hardwareVersion,
                                                                        byte hardwareRevision) throws
                                                                                               IOException
                        {
                            if (m_stealthPowerBootloadVersion != bootloadVersion || m_stealthPowerHardwareVersion != hardwareVersion || m_stealthPowerHardwareRevision != hardwareRevision)
                            {
                                m_stealthpowerLastBootLoaderDetection = null;
                                m_stealthPowerBootloadVersion         = bootloadVersion;
                                m_stealthPowerHardwareVersion         = hardwareVersion;
                                m_stealthPowerHardwareRevision        = hardwareRevision;
                            }

                            if (TimeUtils.isTimeoutExpired(m_stealthpowerLastBootLoaderDetection))
                            {
                                m_stealthPowerOfferedFirmware         = false;
                                m_stealthpowerLastBootLoaderDetection = TimeUtils.computeTimeoutExpiration(1, TimeUnit.HOURS);

                                LoggerInstance.warn("Detected StealthPower MCU bootloader!! (bootloadVersion=%02x, hardwareVersion=%d, hardwareRevision=%d)",
                                                    bootloadVersion,
                                                    hardwareVersion,
                                                    hardwareRevision);
                            }

                            if (!m_stealthPowerOfferedFirmware)
                            {
                                StealthPowerFirmware val = StealthPowerFirmware.matchBootloader(bootloadVersion, hardwareVersion, hardwareRevision);
                                if (val != null)
                                {
                                    boolean shouldInitiate = false;

                                    if (m_stealthPowerOfferingFirmware)
                                    {
                                        StealthPowerManager.LoggerInstance.info("Initiating upgrade, since we offered a new version...");

                                        shouldInitiate = true;
                                    }
                                    else if (!val.supportsRestart)
                                    {
                                        StealthPowerManager.LoggerInstance.info("Initiating upgrade, since bootloader does not support on-demand reflashing...");

                                        shouldInitiate = true;
                                    }
                                    else
                                    {
                                        if (m_stealthpowerForceUpgrade == null)
                                        {
                                            // Wait a bit before forcing the upgrade, in case the app starts normally.
                                            m_stealthpowerForceUpgrade = TimeUtils.computeTimeoutExpiration(30, TimeUnit.SECONDS);
                                        }

                                        if (TimeUtils.isTimeoutExpired(m_stealthpowerForceUpgrade))
                                        {
                                            m_stealthpowerForceUpgrade = null;

                                            StealthPowerManager.LoggerInstance.info("Initiating upgrade, detected bootloader before application...");

                                            shouldInitiate = true;
                                        }
                                    }

                                    if (shouldInitiate)
                                    {
                                        try (InputStream stream = Resources.openResourceAsStream(IpnState.class, val.resource))
                                        {
                                            byte[] payload = IOUtils.toByteArray(stream);
                                            m_stealthPowerOfferedFirmware = true;
                                            return payload;
                                        }
                                    }
                                }
                            }

                            return null;
                        }

                        @Override
                        protected void completedStealthPowerBootloader(int statusCode)
                        {
                            LoggerInstance.info("StealthPower MCU bootloader result: %d", statusCode);
                        }

                        private void handleFirmwareUpgrade(Class<? extends BaseStealthPowerModel> modelClass,
                                                           String version)
                        {
                            if (!StringUtils.equals(m_stealthPowerLastAppVersion, version))
                            {
                                m_stealthPowerLastAppVersion = version;
                                StealthPowerManager.LoggerInstance.info("Detected App Version %s", m_stealthPowerLastAppVersion);
                            }

                            StealthPowerManager mgr = accessSubManager(StealthPowerManager.class);
                            if (mgr != null)
                            {
                                if (StealthPowerFirmware.matchApplication(modelClass, version) == null)
                                {
                                    if (m_stealthPowerReflashDelay == null)
                                    {
                                        m_stealthPowerReflashDelay = TimeUtils.computeTimeoutExpiration(1, TimeUnit.MINUTES);
                                    }

                                    if (TimeUtils.isTimeoutExpired(m_stealthPowerReflashDelay))
                                    {
                                        if (!m_stealthPowerOfferingFirmware)
                                        {
                                            m_stealthPowerOfferingFirmware = true;
                                            StealthPowerManager.LoggerInstance.info("Offering new StealthPower firmware (detected old %s version)", version);
                                        }

                                        mgr.sendResetRequest();
                                    }
                                }
                                else
                                {
                                    if (m_stealthPowerOfferingFirmware)
                                    {
                                        StealthPowerManager.LoggerInstance.info("Successfully updated StealthPower firmware to %s!!!", version);
                                        m_stealthPowerOfferingFirmware = false;
                                        m_stealthPowerReflashDelay     = null;
                                    }
                                }
                            }
                        }
                    };

                    manager.start();
                    m_manager = manager;
                    return wrapAsync(true);
                }
            }

            return wrapAsync(false);
        }
    }

    @Override
    CompletableFuture<Void> stop() throws
                                   Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            if (m_manager != null)
            {
                LoggerInstance.info("Stopping IPN manager on network '%s'", m_configuration.name);

                m_manager.close();
                m_manager = null;

                LoggerInstance.info("Stopped IPN manager on network '%s'", m_configuration.name);
            }

            m_samplingState     = null;
            m_samplingStateNext = null;
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to stop IPN manager on network '%s', due to %s", m_configuration.name, t);
            throw t;
        }

        return wrapAsync(null);
    }

    @Override
    void enumerateNetworkStatistics(BiConsumerWithException<BaseAssetDescriptor, TransportPerformanceCounters> callback)
    {
        // Nothing to do.
    }

    //--//

    @Override
    CompletableFuture<Boolean> reload(ConsumerWithException<ConsumerWithException<InputStream>> stateProcessor) throws
                                                                                                                Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            AtomicReference<SamplingEntries> configRef = new AtomicReference<>();

            stateProcessor.accept((inputStream) -> configRef.set(ObjectMappers.SkipNulls.readValue(inputStream, SamplingEntries.class)));

            SamplingEntries config = configRef.get();
            if (config != null)
            {
                m_samplingStateNext = new SamplingConfig<>();

                for (SamplingEntry entry : config.entries)
                {
                    m_samplingStateNext.add(entry.period, entry.deviceId, entry.objId, Lists.newArrayList(entry.prop), null);
                }

                completeSamplingConfiguration(false, null);
            }

            return wrapAsync(true);
        }
    }

    @Override
    CompletableFuture<Boolean> discover(GatewayOperationTracker.State operationContext,
                                        GatewayState.ResultHolder holder_protocol,
                                        int broadcastIntervals,
                                        int rebroadcastCount) throws
                                                              Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            if (m_manager != null)
            {
                LoggerInstance.info("Started discovery on network '%s'", m_configuration.name);

                try
                {
                    await(m_manager.getReadySignal(), 2, TimeUnit.MINUTES);
                }
                catch (Throwable e)
                {
                    LoggerInstance.warn("IPN manager did not reach ready state before timeout...");
                }

                IpnObjectsState state = getState();

                LoggerInstance.info("Discovery completed on network '%s': found %d values:", m_configuration.name, state.size());

                state.enumerateValues(true, (obj) ->
                {
                    LoggerInstance.info("   %s", obj.extractId());

                    if (obj.shouldIncludeObject())
                    {
                        IpnDeviceDescriptor desc = new IpnDeviceDescriptor();
                        desc.name = obj.extractId();

                        GatewayState.ResultHolder holder_device = holder_protocol.prepareResult(GatewayDiscoveryEntitySelector.Ipn_Device, desc, true);
                        holder_device.queueContents(null);
                    }
                });

                return wrapAsync(true);
            }
        }

        return wrapAsync(false);
    }

    private IpnObjectsState getState()
    {
        if (m_manager != null)
        {
            return m_manager.getState();
        }

        return new IpnManager.State();
    }

    //--//

    @Override
    CompletableFuture<Boolean> listObjects(GatewayOperationTracker.State operationContext,
                                           GatewayState.ResultHolder holder_protocol,
                                           GatewayDiscoveryEntity en_protocol) throws
                                                                               Exception
    {
        LoggerInstance.info("Started listing objects on network '%s'", m_configuration.name);

        await(analyzeDevices(holder_protocol, false));

        LoggerInstance.info("Done listing objects on network '%s'", m_configuration.name);

        return wrapAsync(true);
    }

    @Override
    CompletableFuture<Boolean> readAllValues(GatewayOperationTracker.State operationContext,
                                             GatewayState.ResultHolder holder_protocol,
                                             GatewayDiscoveryEntity en_protocol) throws
                                                                                 Exception
    {
        LoggerInstance.info("Started reading values from objects on network '%s'", m_configuration.name);

        await(analyzeDevices(holder_protocol, true));

        LoggerInstance.info("Done reading values from objects on network '%s'", m_configuration.name);

        return wrapAsync(true);
    }

    @Override
    CompletableFuture<Boolean> writeValues(GatewayOperationTracker.State operationContext,
                                           GatewayState.ResultHolder holder_protocol,
                                           GatewayDiscoveryEntity en_protocol) throws
                                                                               Exception
    {
        return wrapAsync(false); // Not implemented yet.
    }

    private CompletableFuture<Void> analyzeDevices(GatewayState.ResultHolder holder_protocol,
                                                   boolean readContents) throws
                                                                         Exception
    {
        IpnObjectsState state = getState();

        state.enumerateValues(false, (obj) ->
        {
            if (obj.shouldIncludeObject())
            {
                IpnDeviceDescriptor desc = new IpnDeviceDescriptor();
                desc.name = obj.extractId();
                String contents = readContents ? obj.serializeToJson() : null;

                GatewayState.ResultHolder holder_device = holder_protocol.prepareResult(GatewayDiscoveryEntitySelector.Ipn_Device, desc, false);

                for (FieldModel fieldModel : obj.getDescriptors())
                {
                    String fieldName = fieldModel.name;

                    if (obj.shouldIncludeProperty(fieldName))
                    {
                        GatewayState.ResultHolder holder_object = holder_device.prepareResult(GatewayDiscoveryEntitySelector.Ipn_Object, fieldName, true);

                        holder_object.queueContents(contents);
                    }
                }
            }
        });

        return wrapAsync(null);
    }

    //--//

    @Override
    CompletableFuture<Void> startSamplingConfiguration(GatewayOperationTracker.State operationContext) throws
                                                                                                       Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            LoggerInstance.info("Started configuring sampling for objects on network '%s'", m_configuration.name);

            m_samplingStateNext = new SamplingConfig<>();

            ProtocolConfigForIpn cfg = m_configuration.getProtocolConfiguration(ProtocolConfigForIpn.class);
            cfg.samplingConfigurationId = null;

            return wrapAsync(null);
        }
    }

    @Override
    CompletableFuture<Void> updateSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                        GatewayDiscoveryEntity en_protocol) throws
                                                                                            Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            if (m_samplingStateNext != null)
            {
                IpnObjectsState state = getState();

                for (GatewayDiscoveryEntity en_device : en_protocol.filter(GatewayDiscoveryEntitySelector.Ipn_Device))
                {
                    IpnDeviceDescriptor desc = en_device.getSelectorValueAsObject(IpnDeviceDescriptor.class);

                    IpnObjectModel obj = IpnObjectModel.allocateFromDescriptor(desc);
                    if (obj != null)
                    {
                        if (state.getById(obj) == null)
                        {
                            state.set(obj);
                        }
                    }

                    await(configureDevice(m_samplingStateNext, en_device, desc));
                }
            }

            return wrapAsync(null);
        }
    }

    private CompletableFuture<Void> configureDevice(SamplingConfig<String, String, String> samplingState,
                                                    GatewayDiscoveryEntity en_device,
                                                    IpnDeviceDescriptor desc)
    {
        LoggerInstance.info("Configuring device %s...", desc.name);

        int objectsWithSampling = 0;

        for (GatewayDiscoveryEntity en_object : en_device.filter(GatewayDiscoveryEntitySelector.Ipn_ObjectConfig))
        {
            String objId = en_object.selectorValue;

            try
            {
                LoggerInstance.debug("Configuring object %s...", objId);

                Multimap<Integer, String> perPeriod = ArrayListMultimap.create();

                Map<String, Integer> map = en_object.getContentsForObjectConfig(ObjectMappers.SkipNulls);
                for (String prop : map.keySet())
                {
                    int period = map.get(prop);

                    perPeriod.put(period, prop);
                }

                for (Integer period : perPeriod.keySet())
                {
                    samplingState.add(period, desc.name, objId, perPeriod.get(period), null);
                }

                if (perPeriod.size() > 0)
                {
                    objectsWithSampling++;
                }
            }
            catch (Exception e)
            {
                LoggerInstance.warn("Failed to configure object %s, due to exception: %s", objId, e);
            }
        }

        LoggerInstance.info("Configured device %s, Gateway will sample %d objects", desc.name, objectsWithSampling);

        return wrapAsync(null);
    }

    //--//

    @Override
    CompletableFuture<Void> completeSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                          String configId) throws
                                                                           Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            completeSamplingConfiguration(true, configId);

            return wrapAsync(null);
        }
    }

    private void completeSamplingConfiguration(boolean saveConfiguration,
                                               String configId) throws
                                                                Exception
    {
        SamplingConfig<String, String, String> samplingState = m_samplingStateNext;
        if (samplingState != null)
        {
            Multimap<String, String> targets      = HashMultimap.create();
            SamplingEntries          configToSave = saveConfiguration ? new SamplingEntries() : null;

            synchronized (m_newObjectsSeenWhileSampling)
            {
                m_newObjectsSeenWhileSampling.clear();

                samplingState.enumerate(-1, (period, deviceId, objId, prop) ->
                {
                    if (configToSave != null)
                    {
                        SamplingEntry entry = new SamplingEntry();
                        entry.period   = period;
                        entry.deviceId = deviceId;
                        entry.objId    = objId;
                        entry.prop     = prop;
                        configToSave.entries.add(entry);
                    }

                    targets.put(deviceId, objId);

                    // Mark known objects are already seen.
                    m_newObjectsSeenWhileSampling.add(deviceId);
                });
            }

            m_samplingTargets   = targets;
            m_samplingState     = samplingState;
            m_samplingStateNext = null;

            queueNextSampling(samplingState, -1);

            LoggerInstance.info("Done configuring sampling for objects on network '%s'", m_configuration.name);

            if (saveConfiguration)
            {
                ProtocolConfigForIpn cfg = m_configuration.getProtocolConfiguration(ProtocolConfigForIpn.class);
                cfg.samplingConfigurationId = configId;
                m_gatewayState.saveConfiguration(m_configuration, cfg, (output) -> ObjectMappers.SkipNulls.writeValue(output, configToSave));
            }
        }
    }

    private void queueNextSampling(SamplingConfig<String, String, String> samplingState,
                                   int selectPeriod) throws
                                                     Exception
    {
        for (Integer period : samplingState.getPeriods())
        {
            if (selectPeriod == -1 || selectPeriod == period)
            {
                long            now = TimeUtils.nowEpochSeconds();
                SamplingRequest sr  = new SamplingRequest(now, period);

                long nextSample = samplingState.computeNextWakeup(sr);
                if (TimeUtils.isValid(nextSample))
                {
                    long nowMilliUtc      = TimeUtils.nowMilliUtc();
                    long diffMilliseconds = Math.max(0, (nextSample * 1_000 - nowMilliUtc));

                    LoggerInstance.debug("Queued next sampling for period %d at %s", period, nextSample);

                    executeSampling(samplingState, nextSample, period, diffMilliseconds, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    @AsyncBackground
    private CompletableFuture<Void> executeSampling(SamplingConfig<String, String, String> samplingState,
                                                    long samplingSlot,
                                                    int period,
                                                    @AsyncDelay long delay,
                                                    @AsyncDelay TimeUnit delayUnit)
    {
        Multimap<String, String> targets = m_samplingTargets;

        if (samplingState != m_samplingState)
        {
            // Something reset the timer, bail out.
            return wrapAsync(null);
        }

        IpnObjectsState state = getState();

        try
        {
            samplingState.prepareBatches(samplingSlot, period, (deviceId, objId, prop) ->
            {
                // Nothing to do, just to manage sampling state properly.
            });

            samplingState.doneWithCurrentSample(period);

            queueNextSampling(samplingState, period);

            //--//

            state.enumerateValues(false, (obj) ->
            {
                String deviceId = obj.extractId();
                if (targets.containsKey(deviceId))
                {
                    for (FieldModel fieldModel : obj.getDescriptors())
                    {
                        String                    fieldName  = fieldModel.name;
                        IpnObjectModel.FieldState fieldState = obj.getFieldState(fieldName);

                        IpnObjectModel.FieldSample sample;
                        while ((sample = fieldState.pop(samplingSlot)) != null)
                        {
                            queueSample(sample.timestampEpochSeconds, sample.temporalResolution, obj, fieldName, sample.value, false, sample.flushOnChange);
                        }

                        IpnObjectModel.FieldSample lastSample = fieldState.getLastValue();
                        boolean                    stale;

                        if (lastSample == null)
                        {
                            stale = true;
                        }
                        else if (lastSample.emitted && lastSample.timestampEpochSeconds + period * 2 < samplingSlot)
                        {
                            stale = true;
                        }
                        else
                        {
                            stale = false;
                        }

                        if (stale)
                        {
                            // Detected stale value, send notification.
                            queueSample(samplingSlot, FieldTemporalResolution.Max1Hz, obj, fieldName, null, true, false);
                        }
                        else
                        {
                            // Don't use the sample's value, it could be the future value of the field after debouncing.
                            queueSample(samplingSlot, FieldTemporalResolution.Max1Hz, obj, fieldName, fieldState.getCurrentValue(), false, false);
                        }
                    }
                }
            });
        }
        catch (Exception e)
        {
            LoggerInstance.error("Encountered a problem while sampling: %s", e);
        }

        m_gatewayState.startFlushingOfEntities(false);

        return wrapAsync(null);
    }

    private void queueSample(double timestampEpochSeconds,
                             FieldTemporalResolution temporalResolution,
                             IpnObjectModel value,
                             String field,
                             Object fieldValue,
                             boolean assumeMissing,
                             boolean flushOnChange)
    {
        try
        {
            final double truncateTimestamp = temporalResolution.truncateTimestamp(timestampEpochSeconds);

            Multimap<String, String> targets = m_samplingTargets;
            if (targets != null)
            {
                String deviceId = value.extractId();

                Collection<String> objects = targets.get(deviceId);
                boolean            include = objects.contains(field);

                if (LoggerInstanceSamples.isEnabled(Severity.Debug))
                {
                    String id     = String.format("%s/%s", deviceId, field);
                    String prefix = include ? "INCLUDED" : "EXCLUDED";

                    if (assumeMissing)
                    {
                        LoggerInstanceSamples.debug("queueSample: %-80s : %s : MISSING", id, prefix);
                    }
                    else
                    {
                        LoggerInstanceSamples.debug("queueSample: %-80s : %s : %s", id, prefix, fieldValue);
                    }
                }

                if (include)
                {
                    GatewayState.ResultHolder holder_device = getDeviceHolder(truncateTimestamp, deviceId, false);
                    GatewayState.ResultHolder holder_object = holder_device.prepareResult(GatewayDiscoveryEntitySelector.Ipn_Object, field, false);
                    GatewayState.ResultHolder holder_sample = holder_object.prepareResult(GatewayDiscoveryEntitySelector.Ipn_ObjectSample, (String) null, true);

                    if (assumeMissing)
                    {
                        holder_sample.queueContents(null);

                        value.markAsUnreachable();
                    }
                    else
                    {
                        IpnObjectModel valueCopy = BaseObjectModel.copySingleProperty(value, null);
                        valueCopy.setField(field, fieldValue);
                        holder_sample.queueContents(valueCopy.serializeToJson());

                        value.markAsReachable();
                    }

                    value.reportReachabilityChange((isReachable, lastReachable) ->
                                                   {
                                                       if (!value.shouldCommitReachabilityChange(isReachable, lastReachable))
                                                       {
                                                           // Sensor doesn't care for unreachable states.
                                                           return false;
                                                       }

                                                       GatewayState.ResultHolder holder_deviceUnreachable = holder_device.prepareResult(GatewayDiscoveryEntitySelector.Ipn_Reachability,
                                                                                                                                        (String) null,
                                                                                                                                        true);

                                                       DeviceReachability report = new DeviceReachability();
                                                       report.reachable     = isReachable;
                                                       report.lastReachable = lastReachable;

                                                       holder_deviceUnreachable.queueContents(ObjectMappers.SkipNulls.writeValueAsString(report));
                                                       return true;
                                                   });

                    if (flushOnChange)
                    {
                        m_gatewayState.startFlushingOfEntities(true);
                    }
                }
                else if (value.shouldIncludeObject())
                {
                    boolean notSeen;

                    synchronized (m_newObjectsSeenWhileSampling)
                    {
                        notSeen = m_newObjectsSeenWhileSampling.add(deviceId);
                    }

                    if (notSeen)
                    {
                        LoggerInstanceSamples.warn("Detected new object post-discovery: %s", deviceId);

                        //--//

                        IpnObjectsState state = getState();

                        state.enumerateValues(false, (obj) ->
                        {
                            if (StringUtils.equals(obj.extractId(), deviceId))
                            {
                                GatewayState.ResultHolder holder_device = getDeviceHolder(truncateTimestamp, deviceId, true);
                                holder_device.queueContents(null);

                                String contents = obj.serializeToJson();

                                for (FieldModel fieldModel : obj.getDescriptors())
                                {
                                    String fieldName = fieldModel.name;

                                    if (obj.shouldIncludeProperty(fieldName))
                                    {
                                        GatewayState.ResultHolder holder_object = holder_device.prepareResult(GatewayDiscoveryEntitySelector.Ipn_Object, fieldName, true);

                                        holder_object.queueContents(contents);
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }
        catch (Exception e)
        {
            LoggerInstance.error("Encountered a problem while queuing sampling: %s", e);
        }
    }

    private GatewayState.ResultHolder getDeviceHolder(double timestampEpochSeconds,
                                                      String deviceId,
                                                      boolean addTimestamp) throws
                                                                            JsonProcessingException
    {
        GatewayState.ResultHolder holder_root     = m_gatewayState.getRoot(timestampEpochSeconds);
        GatewayState.ResultHolder holder_network  = holder_root.prepareResult(GatewayDiscoveryEntitySelector.Network, m_configuration.sysId, false);
        GatewayState.ResultHolder holder_protocol = holder_network.prepareResult(GatewayDiscoveryEntitySelector.Protocol, GatewayDiscoveryEntity.Protocol_Ipn, false);

        IpnDeviceDescriptor desc = new IpnDeviceDescriptor();
        desc.name = deviceId;

        return holder_protocol.prepareResult(GatewayDiscoveryEntitySelector.Ipn_Device, desc, addTimestamp);
    }
}