/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.state;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.asyncawait.AsyncDelay;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayOperationTracker;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayState;
import com.optio3.cloud.client.gateway.orchestration.state.SamplingConfig;
import com.optio3.cloud.client.gateway.orchestration.state.SamplingRequest;
import com.optio3.cloud.hub.logic.simulator.SimulatedGateway;
import com.optio3.cloud.hub.logic.simulator.generators.DeviceGenerator;
import com.optio3.cloud.hub.logic.simulator.generators.ObjectGenerator;
import com.optio3.concurrency.AsyncMutex;
import com.optio3.logging.Logger;
import com.optio3.protocol.model.bacnet.BACnetDeviceAddress;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.config.ProtocolConfigForBACnet;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;

class SimulatedBACnetState
{
    private static final Logger LoggerInstance            = SimulatedGateway.LoggerInstance.createSubLogger(SimulatedBACnetState.class);
    private static final int    HistoricalSampleBatchSize = 20000;

    private final AsyncMutex m_mutex = new AsyncMutex();

    private final SimulatedGateway m_gateway;
    private final GatewayState     m_gatewayState;
    private final GatewayNetwork   m_configuration;

    private List<DeviceGenerator> m_discoveredDevices;
    private boolean               m_reportedDevices;
    private boolean               m_objectsListed;

    private SamplingConfig<BACnetDeviceAddress, BACnetObjectIdentifier, BACnetPropertyIdentifierOrUnknown> m_samplingState;
    private SamplingConfig<BACnetDeviceAddress, BACnetObjectIdentifier, BACnetPropertyIdentifierOrUnknown> m_samplingStateNext;
    private int                                                                                            m_sampleCount;

    //--//

    SimulatedBACnetState(SimulatedGateway gateway,
                         GatewayNetwork configuration)
    {
        m_gateway       = gateway;
        m_gatewayState  = gateway.getState();
        m_configuration = configuration;
    }

    //--//

    CompletableFuture<Boolean> start() throws
                                       Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            return wrapAsync(true);
        }
    }

    CompletableFuture<Void> stop() throws
                                   Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            m_samplingState = null;

            return wrapAsync(null);
        }
    }

    //--//

    CompletableFuture<Boolean> discover(GatewayOperationTracker.State operationContext,
                                        GatewayState.ResultHolder holder_protocol,
                                        int broadcastIntervals,
                                        int rebroadcastCount) throws
                                                              Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            if (m_discoveredDevices == null)
            {
                LoggerInstance.info("Started simulated discovery on network '%s'", m_configuration.name);

                m_objectsListed   = false;
                m_reportedDevices = false;

                m_discoveredDevices = m_gateway.getDevices();

                LoggerInstance.info("Discovery completed on network '%s': found %d devices", m_configuration.name, m_discoveredDevices.size());
            }

            if (!m_reportedDevices)
            {
                try
                {
                    for (DeviceGenerator gen : m_discoveredDevices)
                    {
                        BACnetDeviceDescriptor desc = new BACnetDeviceDescriptor();
                        desc.address = gen.getDeviceAddress();

                        GatewayState.ResultHolder holder_device = holder_protocol.prepareResult(GatewayDiscoveryEntitySelector.BACnet_Device, desc, true);
                        await(analyzeObject(holder_device, gen, false));
                    }

                    m_reportedDevices = true;
                }
                catch (Throwable t)
                {
                    LoggerInstance.warn("Failed to publish device list to Hub: %s", t);
                }
            }

            return wrapAsync(true);
        }
    }

    //--//

    CompletableFuture<Boolean> listObjects(GatewayOperationTracker.State operationContext,
                                           GatewayState.ResultHolder holder_protocol,
                                           GatewayDiscoveryEntity en_protocol) throws
                                                                               Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            if (m_discoveredDevices != null && !m_objectsListed)
            {
                LoggerInstance.info("Started listing simulated objects on network '%s'", m_configuration.name);

                await(analyzeDevices(operationContext, holder_protocol, en_protocol, false, true));

                LoggerInstance.info("Done listing simulated objects on network '%s'", m_configuration.name);

                m_objectsListed = true;
            }

            return wrapAsync(true);
        }
    }

    CompletableFuture<Boolean> readAllValues(GatewayOperationTracker.State operationContext,
                                             GatewayState.ResultHolder holder_protocol,
                                             GatewayDiscoveryEntity en_protocol) throws
                                                                                 Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            if (m_discoveredDevices != null)
            {
                LoggerInstance.info("Started reading simulated values from objects on network '%s'", m_configuration.name);

                await(analyzeDevices(operationContext, holder_protocol, en_protocol, true, true));

                LoggerInstance.info("Done reading simulated values from objects on network '%s'", m_configuration.name);
            }

            return wrapAsync(true);
        }
    }

    private CompletableFuture<Void> analyzeDevices(GatewayOperationTracker.State operationContext,
                                                   GatewayState.ResultHolder holder_protocol,
                                                   GatewayDiscoveryEntity en_protocol,
                                                   boolean readContents,
                                                   boolean inParallel) throws
                                                                       Exception
    {
        if (en_protocol.subEntities == null)
        {
            for (DeviceGenerator deviceGen : m_discoveredDevices)
            {
                BACnetDeviceDescriptor desc = new BACnetDeviceDescriptor();
                desc.address = deviceGen.getDeviceAddress();

                en_protocol.createAsRequest(GatewayDiscoveryEntitySelector.BACnet_Device, desc);
            }
        }

        List<CompletableFuture<Void>> pendingDevices = Lists.newArrayList();

        for (GatewayDiscoveryEntity en_device : en_protocol.filter(GatewayDiscoveryEntitySelector.BACnet_Device))
        {
            BACnetDeviceDescriptor desc = en_device.getSelectorValueAsObject(BACnetDeviceDescriptor.class);
            if (desc == null || desc.address == null)
            {
                continue;
            }

            DeviceGenerator         deviceGen = getDeviceGenerator(desc.address);
            CompletableFuture<Void> res       = analyzeDevice(operationContext, holder_protocol, en_device, readContents, deviceGen);

            if (inParallel)
            {
                pendingDevices.add(res);
            }
            else
            {
                await(res);
            }
        }

        //
        // First pass just to make sure we wait for everything to be done.
        //
        for (CompletableFuture<Void> pendingDevice : pendingDevices)
        {
            try
            {
                await(pendingDevice);
            }
            catch (Throwable t)
            {
                // Ignore for now.
            }
        }

        //
        // Second pass to actually throw exceptions.
        //
        for (CompletableFuture<Void> pendingDevice : pendingDevices)
        {
            await(pendingDevice);
        }

        return wrapAsync(null);
    }

    private CompletableFuture<Void> analyzeDevice(GatewayOperationTracker.State operationContext,
                                                  GatewayState.ResultHolder holder_protocol,
                                                  GatewayDiscoveryEntity en_device,
                                                  boolean readContents,
                                                  DeviceGenerator deviceGen) throws
                                                                             Exception
    {
        LoggerInstance.info("Analyzing simulated device %s...", deviceGen.getIdentifier());

        BACnetDeviceDescriptor    desc          = en_device.getSelectorValueAsObject(BACnetDeviceDescriptor.class);
        GatewayState.ResultHolder holder_device = holder_protocol.prepareResult(GatewayDiscoveryEntitySelector.BACnet_Device, desc, false);

        try
        {
            int objectsAnalyzed = 0;

            if (en_device.subEntities != null)
            {
                for (GatewayDiscoveryEntity en_object : en_device.filter(GatewayDiscoveryEntitySelector.BACnet_ObjectConfig))
                {
                    BACnetObjectIdentifier objId  = new BACnetObjectIdentifier(en_object.selectorValue);
                    ObjectGenerator<?>     objGen = deviceGen.getObjectGenerator(objId);
                    if (objGen != null)
                    {
                        await(analyzeObject(holder_device, objGen, readContents));
                        objectsAnalyzed++;
                    }
                }
            }
            else
            {
                Collection<ObjectGenerator<?>> objectGenerators = deviceGen.getObjectGenerators();
                for (ObjectGenerator<?> objGen : objectGenerators)
                {
                    await(analyzeObject(holder_device, objGen, readContents));
                    objectsAnalyzed++;
                }
            }

            LoggerInstance.info("Analyzed device %s, found %d objects", deviceGen.getIdentifier(), objectsAnalyzed);
        }
        catch (Throwable t)
        {
            LoggerInstance.info("Failed to analyze device %s, due to %s", deviceGen.getIdentifier(), t);
        }

        return wrapAsync(null);
    }

    private CompletableFuture<Void> analyzeObject(GatewayState.ResultHolder holder_device,
                                                  ObjectGenerator<?> generator,
                                                  boolean readContents)
    {
        LoggerInstance.debug("Analyzing simulated object %s...", generator.getIdentifier());

        String contents = null;

        if (readContents)
        {
            try
            {
                BACnetObjectModel result = generator.readAllProperties(TimeUtils.now(), false);

                contents = result.serializeToJson();
            }
            catch (Throwable t)
            {
                LoggerInstance.info("Failed to read contents of object %s on device %s, due to %s");
            }
        }

        GatewayState.ResultHolder holder_object = holder_device.prepareResult(GatewayDiscoveryEntitySelector.BACnet_Object,
                                                                              generator.getIdentifier()
                                                                                       .toJsonValue(),
                                                                              true);

        holder_object.queueContents(contents);

        return wrapAsync(null);
    }

    //--//

    CompletableFuture<Void> startSamplingConfiguration(GatewayOperationTracker.State operationContext) throws
                                                                                                       Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            m_samplingStateNext = new SamplingConfig<>();

            ProtocolConfigForBACnet cfg = m_configuration.getProtocolConfiguration(ProtocolConfigForBACnet.class);
            cfg.samplingConfigurationId = null;

            return wrapAsync(null);
        }
    }

    CompletableFuture<Void> updateSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                        GatewayDiscoveryEntity en_protocol) throws
                                                                                            Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            if (m_discoveredDevices != null)
            {
                LoggerInstance.info("Started configuring sampling for simulated objects on network '%s'", m_configuration.name);

                for (GatewayDiscoveryEntity en_device : en_protocol.filter(GatewayDiscoveryEntitySelector.BACnet_Device))
                {
                    BACnetDeviceDescriptor desc = en_device.getSelectorValueAsObject(BACnetDeviceDescriptor.class);
                    if (desc == null || desc.address == null)
                    {
                        continue;
                    }

                    DeviceGenerator deviceGen = getDeviceGenerator(desc.address);
                    if (deviceGen == null)
                    {
                        continue;
                    }

                    await(configureDevice(m_samplingStateNext, en_device, deviceGen));
                }

                LoggerInstance.info("Done configuring sampling for simulated objects on network '%s'", m_configuration.name);
            }

            return wrapAsync(null);
        }
    }

    CompletableFuture<Void> completeSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                          String configId) throws
                                                                           Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            m_samplingState = m_samplingStateNext;

            ProtocolConfigForBACnet cfg = m_configuration.getProtocolConfiguration(ProtocolConfigForBACnet.class);
            cfg.samplingConfigurationId = configId;

            queueNextSampling(m_samplingState, -1);

            return wrapAsync(null);
        }
    }

    private void queueNextSampling(SamplingConfig<BACnetDeviceAddress, BACnetObjectIdentifier, BACnetPropertyIdentifierOrUnknown> samplingState,
                                   int selectPeriod) throws
                                                     Exception
    {
        if (samplingState != null)
        {
            long          now                     = TimeUtils.nowEpochSeconds();
            ZonedDateTime historicalDataStartDate = m_gateway.getHistoricalDataStartDate();

            if (historicalDataStartDate != null)
            {
                now = Math.min(now, historicalDataStartDate.toEpochSecond());
            }

            for (Integer period : samplingState.getPeriods())
            {
                if (selectPeriod == -1 || selectPeriod == period)
                {
                    SamplingRequest sr = new SamplingRequest(now, period);

                    long nextSample = samplingState.computeNextWakeup(sr);
                    if (TimeUtils.isValid(nextSample))
                    {
                        long nowMilliUtc      = TimeUtils.nowMilliUtc();
                        long diffMilliseconds = Math.max(0, (nextSample * 1_000 - nowMilliUtc));

                        LoggerInstance.debug("Queued next sampling for period %d at %s", period, nextSample);

                        if (historicalDataStartDate != null)
                        {
                            if (nextSample >= TimeUtils.nowEpochSeconds())
                            {
                                // We've caught up, unset to go back to normal generation
                                m_gateway.setHistoricalDataStartDate(null);
                            }
                            else
                            {
                                m_gateway.setHistoricalDataStartDate(TimeUtils.fromSecondsToUtcTime(nextSample));
                            }
                        }

                        executeSampling(samplingState, nextSample, period, diffMilliseconds, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }

    @AsyncBackground
    private CompletableFuture<Void> executeSampling(SamplingConfig<BACnetDeviceAddress, BACnetObjectIdentifier, BACnetPropertyIdentifierOrUnknown> samplingState,
                                                    long samplingSlot,
                                                    int period,
                                                    @AsyncDelay long delay,
                                                    @AsyncDelay TimeUnit delayUnit) throws
                                                                                    Exception
    {
        if (samplingState != m_samplingState)
        {
            // Something reset the timer, bail out.
            return wrapAsync(null);
        }

        ZonedDateTime                                          now           = TimeUtils.fromSecondsToLocalTime(samplingSlot);
        Map<BACnetDeviceAddress, DeviceGenerator.DeviceReader> deviceReaders = Maps.newHashMap();

        LoggerInstance.debug("Sampling Start %s", now);

        try
        {
            samplingState.prepareBatches(samplingSlot, period, (di, objDesc, prop) ->
            {
                DeviceGenerator    deviceGen = getDeviceGenerator(di);
                ObjectGenerator<?> objGen    = deviceGen.getObjectGenerator(objDesc);

                DeviceGenerator.DeviceReader reader = deviceReaders.computeIfAbsent(di, (d) -> deviceGen.newDeviceReader());
                reader.addObjectReader(objDesc, prop, (id) -> objGen.newReader());
            });

            GatewayState.ResultHolder holder_root     = m_gatewayState.getRoot(samplingSlot);
            GatewayState.ResultHolder holder_network  = holder_root.prepareResult(GatewayDiscoveryEntitySelector.Network, m_configuration.sysId, false);
            GatewayState.ResultHolder holder_protocol = holder_network.prepareResult(GatewayDiscoveryEntitySelector.Protocol, GatewayDiscoveryEntity.Protocol_BACnet, false);

            for (BACnetDeviceAddress di : deviceReaders.keySet())
            {
                try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
                {
                    DeviceGenerator.DeviceReader deviceReader = deviceReaders.get(di);

                    BACnetDeviceDescriptor desc = new BACnetDeviceDescriptor();
                    desc.address = di;

                    GatewayState.ResultHolder holder_device = holder_protocol.prepareResult(GatewayDiscoveryEntitySelector.BACnet_Device, desc, false);

                    await(deviceReader.read(holder_device, () ->
                    {
                        m_sampleCount++;

                        return wrapAsync(null);
                    }));
                }
            }

            if (m_gateway.getHistoricalDataStartDate() != null && m_sampleCount > HistoricalSampleBatchSize)
            {
                await(m_gatewayState.waitForFlushingOfEntities());

                m_sampleCount = 0;
            }
        }
        catch (Exception e)
        {
            LoggerInstance.error("Encountered a problem while sampling: %s", e);
        }

        samplingState.doneWithCurrentSample(period);

        queueNextSampling(samplingState, period);

        LoggerInstance.debug("Sampling End");

        m_gatewayState.startFlushingOfEntities(false);

        return wrapAsync(null);
    }

    private DeviceGenerator getDeviceGenerator(BACnetDeviceAddress address)
    {
        return CollectionUtils.findFirst(m_discoveredDevices,
                                         d -> d.getDeviceAddress()
                                               .equals(address));
    }

    private CompletableFuture<Void> configureDevice(SamplingConfig<BACnetDeviceAddress, BACnetObjectIdentifier, BACnetPropertyIdentifierOrUnknown> samplingState,
                                                    GatewayDiscoveryEntity en_device,
                                                    DeviceGenerator deviceGen) throws
                                                                               Exception
    {
        LoggerInstance.info("Configuring device %s...", deviceGen.getIdentifier());

        int objectsWithSampling = 0;

        for (GatewayDiscoveryEntity en_object : en_device.filter(GatewayDiscoveryEntitySelector.BACnet_ObjectConfig))
        {
            BACnetObjectIdentifier objId = new BACnetObjectIdentifier(en_object.selectorValue);

            ObjectGenerator<?> objGen = deviceGen.getObjectGenerator(objId);
            if (objGen != null)
            {
                LoggerInstance.debug("Configuring object %s...", objId);

                final BACnetDeviceAddress                            deviceAddress = deviceGen.getDeviceAddress();
                Multimap<Integer, BACnetPropertyIdentifierOrUnknown> perPeriod     = ArrayListMultimap.create();

                Map<String, Integer> map = en_object.getContentsForObjectConfig(ObjectMappers.SkipNulls);
                for (String prop : map.keySet())
                {
                    int                               period = map.get(prop);
                    BACnetPropertyIdentifierOrUnknown propId = BACnetPropertyIdentifierOrUnknown.parse(prop);

                    perPeriod.put(period, propId);
                }

                for (Integer period : perPeriod.keySet())
                {
                    samplingState.add(period, deviceAddress, objId, perPeriod.get(period), null);
                }

                objectsWithSampling++;
            }
        }

        LoggerInstance.info("Configured device %s, Gateway will sample %d objects", deviceGen.getIdentifier(), objectsWithSampling);

        return wrapAsync(null);
    }
}