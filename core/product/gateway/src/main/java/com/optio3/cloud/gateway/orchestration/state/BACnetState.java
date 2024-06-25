/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.orchestration.state;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.asyncawait.AsyncDelay;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntity;
import com.optio3.cloud.client.gateway.model.GatewayDiscoveryEntitySelector;
import com.optio3.cloud.client.gateway.model.GatewayNetwork;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayOperationTracker;
import com.optio3.cloud.client.gateway.orchestration.state.GatewayState;
import com.optio3.cloud.client.gateway.orchestration.state.SamplingConfig;
import com.optio3.cloud.client.gateway.orchestration.state.SamplingRequest;
import com.optio3.cloud.gateway.GatewayApplication;
import com.optio3.concurrency.AsyncMutex;
import com.optio3.concurrency.AsyncWaitMultiple;
import com.optio3.infra.NetworkHelper;
import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.logging.RedirectingLogger;
import com.optio3.logging.Severity;
import com.optio3.protocol.bacnet.BACnetManager;
import com.optio3.protocol.bacnet.DeviceIdentity;
import com.optio3.protocol.bacnet.TimeoutSpec;
import com.optio3.protocol.bacnet.transport.EthernetTransportBuilder;
import com.optio3.protocol.bacnet.transport.UdpTransportBuilder;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.DeviceReachability;
import com.optio3.protocol.model.TransportPerformanceCounters;
import com.optio3.protocol.model.bacnet.BACnetAddress;
import com.optio3.protocol.model.bacnet.BACnetDeviceAddress;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.constructed.BACnetAddressBinding;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetSegmentation;
import com.optio3.protocol.model.config.BACnetBBMD;
import com.optio3.protocol.model.config.FilteredSubnet;
import com.optio3.protocol.model.config.NonDiscoverableBACnetDevice;
import com.optio3.protocol.model.config.NonDiscoverableMstpTrunk;
import com.optio3.protocol.model.config.ProtocolConfigForBACnet;
import com.optio3.protocol.model.config.SkippedBACnetDevice;
import com.optio3.protocol.model.transport.TransportAddress;
import com.optio3.protocol.model.transport.UdpTransportAddress;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.MonotonousTime;
import com.optio3.util.ResourceCleaner;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.BiConsumerWithException;
import com.optio3.util.function.ConsumerWithException;
import org.apache.commons.lang3.StringUtils;

class BACnetState extends CommonState
{
    static final int MAX_FAILURES_BEFORE_DISABLING = 4;

    static class BatchReaderFailureDetails
    {
        BACnetObjectIdentifier            id;
        BACnetPropertyIdentifierOrUnknown prop;
        Exception                         failure;
    }

    static class BatchReaderStats extends GatewayState.ProgressStatus
    {
        Multimap<Class<?>, BatchReaderFailureDetails> failures;
    }

    static class SharedManager extends ResourceCleaner
    {
        private static final AtomicInteger s_refCounter = new AtomicInteger();
        private static       BACnetManager s_manager;

        private BACnetManager m_manager;

        SharedManager(BACnetState holder)
        {
            super(holder);
        }

        @Override
        protected void closeUnderCleaner()
        {
            if (m_manager != null)
            {
                m_manager = null;

                synchronized (s_refCounter)
                {
                    if (s_refCounter.decrementAndGet() == 0)
                    {
                        s_manager.close();
                        s_manager = null;
                    }
                }
            }
        }

        BACnetManager get()
        {
            if (m_manager == null)
            {
                synchronized (s_refCounter)
                {
                    if (s_refCounter.getAndIncrement() == 0)
                    {
                        s_manager = new BACnetManager();
                    }

                    m_manager = s_manager;
                }
            }

            return m_manager;
        }
    }

    //--//

    public static class SamplingEntry
    {
        public int                               period;
        public BACnetDeviceDescriptor            deviceId;
        public BACnetObjectIdentifier            objId;
        public BACnetPropertyIdentifierOrUnknown prop;
    }

    public static class SamplingEntries
    {
        public final List<SamplingEntry> entries = Lists.newArrayList();
    }

    //--//

    static final Logger LoggerInstance            = GatewayApplication.LoggerInstance.createSubLogger(BACnetState.class);
    static final Logger LoggerInstanceForProgress = LoggerInstance.createSubLogger(GatewayState.ProgressStatus.class);

    private final AsyncMutex m_mutex = new AsyncMutex();

    private final Map<DeviceIdentity, String> m_devicesToIgnore = Maps.newHashMap();
    private       SharedManager               m_sharedManager;
    private       int                         m_udpNetworkPort;

    private final Set<TransportAddress>                                                                     m_samplingTargets = Sets.newConcurrentHashSet();
    private       SamplingConfig<DeviceIdentity, BACnetObjectIdentifier, BACnetPropertyIdentifierOrUnknown> m_samplingState;
    private       SamplingConfig<DeviceIdentity, BACnetObjectIdentifier, BACnetPropertyIdentifierOrUnknown> m_samplingStateNext;

    //--//

    public BACnetState(GatewayState gatewayState,
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

    private BACnetManager getManager()
    {
        SharedManager sharedManager = m_sharedManager;
        if (sharedManager == null)
        {
            return null;
        }

        BACnetManager mgr = sharedManager.get();
        return mgr.isClosed() ? null : mgr;
    }

    //--//

    @Override
    CompletableFuture<Boolean> start() throws
                                       Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            boolean res = await(startUnderLock());
            return wrapAsync(res);
        }
    }

    private CompletableFuture<Boolean> startUnderLock()
    {
        //
        // Initialize the BACnet manager.
        //
        if (m_sharedManager == null)
        {
            ProtocolConfigForBACnet cfg = m_configuration.getProtocolConfiguration(ProtocolConfigForBACnet.class);
            if (cfg != null)
            {
                boolean hasInterface = StringUtils.isNotEmpty(m_configuration.networkInterface);

                LoggerInstance.info("Starting BACnet manager on network '%s'", m_configuration.name);

                m_sharedManager = new SharedManager(this);

                BACnetManager manager = getManager();

                if (cfg.useEthernet)
                {
                    EthernetTransportBuilder transportBuilder = EthernetTransportBuilder.newBuilder();

                    if (hasInterface)
                    {
                        transportBuilder.setDevice(m_configuration.networkInterface);
                    }

                    manager.addTransport(transportBuilder.build());
                }

                m_udpNetworkPort = cfg.networkPort > 0 ? cfg.networkPort : BACnetManager.c_DefaultPort;

                if (cfg.useUDP)
                {
                    UdpTransportBuilder transportBuilder = UdpTransportBuilder.newBuilder();

                    if (hasInterface)
                    {
                        transportBuilder.setDevice(m_configuration.networkInterface);
                    }

                    transportBuilder.setNetworkPort(m_udpNetworkPort);

                    manager.addTransport(transportBuilder.build());
                }

                if (cfg.useUDP)
                {
                    for (BACnetBBMD bbmd : cfg.bbmds)
                    {
                        InetSocketAddress bbmdAddress = new InetSocketAddress(bbmd.networkAddress, bbmd.networkPort > 0 ? bbmd.networkPort : m_udpNetworkPort);
                        manager.addBBMD(new UdpTransportAddress(bbmdAddress));
                    }
                }

                manager.start();
                m_devicesToIgnore.clear();

                LoggerInstance.info("Started BACnet manager on network '%s'", m_configuration.name);

                return wrapAsync(true);
            }
        }

        return wrapAsync(false);
    }

    @Override
    CompletableFuture<Void> stop() throws
                                   Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            await(stopUnderLock());

            return wrapAsync(null);
        }
    }

    private CompletableFuture<Void> stopUnderLock()
    {
        try
        {
            if (m_sharedManager != null)
            {
                LoggerInstance.info("Stopping BACnet manager on network '%s'", m_configuration.name);

                m_sharedManager.clean();
                m_sharedManager = null;
                m_devicesToIgnore.clear();

                LoggerInstance.info("Stopped BACnet manager on network '%s'", m_configuration.name);
            }

            m_samplingState     = null;
            m_samplingStateNext = null;
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to stop BACnet manager on network '%s', due to %s", m_configuration.name, t);
            throw t;
        }

        return wrapAsync(null);
    }

    @Override
    void enumerateNetworkStatistics(BiConsumerWithException<BaseAssetDescriptor, TransportPerformanceCounters> callback) throws
                                                                                                                         Exception
    {
        BACnetManager manager = getManager();
        if (manager != null)
        {
            manager.enumerateNetworkStatistics(((ta, desc, stats) ->
            {
                if (ta == null || m_samplingTargets.contains(ta))
                {
                    callback.accept(desc, stats);
                }
            }));

            m_gatewayState.startFlushingOfEntities(false);
        }
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
                BACnetManager manager = getManager();
                if (manager != null)
                {
                    Function<Set<BACnetPropertyIdentifierOrUnknown>, Set<BACnetPropertyIdentifierOrUnknown>> memoizingCallback = manager::memoizeProperties;

                    for (SamplingEntry entry : config.entries)
                    {
                        DeviceIdentity di = manager.registerDevice(entry.deviceId);
                        if (di != null)
                        {
                            di.startResolution();

                            try
                            {
                                LoggerInstance.debug("Configuring object %s...", entry.objId);

                                DeviceIdentity.ObjectDescriptor objDesc = di.ensureObjectDescriptor(entry.objId);
                                if (objDesc != null)
                                {
                                    m_samplingStateNext.add(entry.period, di, entry.objId, Lists.newArrayList(entry.prop), memoizingCallback);
                                }
                            }
                            catch (Exception e)
                            {
                                LoggerInstance.warn("Failed to configure object %s, due to exception: %s", entry.objId, e);
                            }
                        }
                    }
                }

                completeSamplingConfiguration(LoggerInstance, false, null);
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
        ILogger logger = operationContext.getContextualLogger(LoggerInstance);

        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            BACnetManager manager = getManager();
            if (manager != null)
            {
                m_devicesToIgnore.clear();

                logger.info("Started discovery on network '%s'", m_configuration.name);

                ProtocolConfigForBACnet cfg = m_configuration.getProtocolConfiguration(ProtocolConfigForBACnet.class);
                if (cfg != null)
                {
                    Multimap<TransportAddress, Integer> nonDiscoverableMstpTrunks;

                    if (cfg.nonDiscoverableMstpTrunks != null)
                    {
                        nonDiscoverableMstpTrunks = HashMultimap.create();

                        for (NonDiscoverableMstpTrunk nonDiscoverableMstpTrunk : cfg.nonDiscoverableMstpTrunks)
                        {
                            UdpTransportAddress ta = new UdpTransportAddress(nonDiscoverableMstpTrunk.networkAddress, nonDiscoverableMstpTrunk.networkPort);

                            nonDiscoverableMstpTrunks.put(ta, nonDiscoverableMstpTrunk.networkNumber);
                        }
                    }
                    else
                    {
                        nonDiscoverableMstpTrunks = null;
                    }

                    Set<DeviceIdentity> discoveredDevices = Sets.newConcurrentHashSet();

                    if (!cfg.disableBroadcast)
                    {
                        Set<DeviceIdentity> broadcastDevices = await(manager.scanForDevices(logger,
                                                                                            rebroadcastCount,
                                                                                            cfg.limitScan,
                                                                                            cfg.sweepMSTP,
                                                                                            cfg.includeNetworksFromRouters,
                                                                                            nonDiscoverableMstpTrunks,
                                                                                            (msg, di) ->
                                                                                            {
                                                                                                logger.debug("  >> Got IAm from %s at %s", msg.i_am_device_identifier, di.getTransportAddress());
                                                                                            }));

                        discoveredDevices.addAll(broadcastDevices);

                        {
                            logger.info("Scan completed on network '%s': found %d devices:", m_configuration.name, broadcastDevices.size());

                            List<DeviceIdentity> sortedDevices = Lists.newArrayList(broadcastDevices);
                            sortedDevices.sort(DeviceIdentity::compareTo);

                            for (DeviceIdentity di : sortedDevices)
                            {
                                logger.info("  %s", di);
                            }
                        }
                    }

                    List<FilteredSubnet> scanSubnets = Lists.newArrayList(cfg.scanSubnets);
                    if (cfg.sweepSubnet)
                    {
                        FilteredSubnet localNet = new FilteredSubnet();
                        localNet.cidr = m_configuration.cidr;
                        scanSubnets.add(localNet);
                    }

                    for (FilteredSubnet scanSubnet : scanSubnets)
                    {
                        AsyncWaitMultiple                   waiter   = new AsyncWaitMultiple();
                        NetworkHelper.InetAddressWithPrefix subnet   = NetworkHelper.InetAddressWithPrefix.parse(scanSubnet.cidr);
                        TimeoutSpec                         timeSpec = TimeoutSpec.create(4, Duration.of(1000, ChronoUnit.MILLIS));

                        LoggerInstance.info("Scanning subnet %s...", subnet);

                        for (int offset = 1; offset < subnet.getSize(); offset++)
                        {
                            InetAddress address = subnet.generateAddress(offset);

                            waiter.add(checkSubnetTarget(discoveredDevices, address, timeSpec));

                            await(sleep(20, TimeUnit.MILLISECONDS));
                        }

                        await(waiter.drain());
                    }

                    Set<DeviceIdentity> nonDiscoverableDevices = Sets.newHashSet();

                    if (cfg.nonDiscoverableDevices != null)
                    {
                        for (NonDiscoverableBACnetDevice nonDiscoverableDevice : cfg.nonDiscoverableDevices)
                        {
                            UdpTransportAddress ta            = new UdpTransportAddress(nonDiscoverableDevice.networkAddress, nonDiscoverableDevice.networkPort);
                            BACnetDeviceAddress deviceAddress = new BACnetDeviceAddress(nonDiscoverableDevice.networkNumber, nonDiscoverableDevice.instanceNumber);
                            BACnetAddress       macAddress    = null;

                            if (nonDiscoverableDevice.mstpAddress > 0)
                            {
                                macAddress = BACnetAddress.createMstp(nonDiscoverableDevice.networkNumber, nonDiscoverableDevice.mstpAddress);
                            }

                            DeviceIdentity di = manager.registerDevice(ta, macAddress, deviceAddress, BACnetSegmentation.no_segmentation, 408);
                            if (di != null)
                            {
                                nonDiscoverableDevices.add(di);
                            }
                        }
                    }

                    {
                        AsyncWaitMultiple waiter   = new AsyncWaitMultiple();
                        TimeoutSpec       timeSpec = TimeoutSpec.create(4, Duration.of(1000, ChronoUnit.MILLIS));

                        LoggerInstance.info("Scanning address bindings...");

                        Set<DeviceIdentity> toProbe = Sets.newHashSet();
                        toProbe.addAll(discoveredDevices);
                        toProbe.addAll(nonDiscoverableDevices);

                        Set<BACnetDeviceAddress> probedAddressBindinds = Sets.newConcurrentHashSet();

                        for (DeviceIdentity di : toProbe)
                        {
                            waiter.add(checkAddressBindings(logger, discoveredDevices, probedAddressBindinds, di, timeSpec));

                            await(sleep(20, TimeUnit.MILLISECONDS));
                        }

                        await(waiter.drain());
                    }

                    if (cfg.skippedDevices != null)
                    {
                        for (SkippedBACnetDevice skippedDevice : cfg.skippedDevices)
                        {
                            BACnetDeviceAddress deviceAddress = new BACnetDeviceAddress(skippedDevice.networkNumber, skippedDevice.instanceNumber);
                            UdpTransportAddress ta;

                            if (skippedDevice.transportAddress != null)
                            {
                                ta = new UdpTransportAddress(skippedDevice.transportAddress, skippedDevice.transportPort);
                            }
                            else
                            {
                                ta = null;
                            }

                            DeviceIdentity di = manager.locateDevice(deviceAddress, ta, false);
                            if (di != null)
                            {
                                m_devicesToIgnore.put(di, "Will be skipped, per configuration");
                            }
                        }
                    }

                    Collection<NetworkHelper.InetAddressWithPrefix> filters = parseFilters(cfg);
                    if (filters != null)
                    {
                        for (DeviceIdentity di : discoveredDevices)
                        {
                            if (m_devicesToIgnore.containsKey(di))
                            {
                                continue; // Already excluded.
                            }

                            if (!isIncludedInFilters(filters, di))
                            {
                                m_devicesToIgnore.put(di, "Will be skipped, not part of configured subnets");
                            }
                        }
                    }

                    {
                        logger.info("Discovery completed on network '%s': found %d devices:", m_configuration.name, discoveredDevices.size());

                        List<DeviceIdentity> sortedDevices = Lists.newArrayList(discoveredDevices);
                        sortedDevices.sort(DeviceIdentity::compareTo);

                        for (DeviceIdentity di : sortedDevices)
                        {
                            String reason = m_devicesToIgnore.get(di);
                            if (reason != null)
                            {
                                logger.info("  %s - %s!!", di, reason);
                            }
                            else
                            {
                                logger.info("  %s", di);
                            }
                        }

                        sortedDevices = Lists.newArrayList(nonDiscoverableDevices);
                        sortedDevices.sort(DeviceIdentity::compareTo);
                        for (DeviceIdentity di : sortedDevices)
                        {
                            logger.info("  %s - Will also be included, per configuration!", di);
                        }
                    }

                    List<DeviceIdentity> devicesToReport = Lists.newArrayList(nonDiscoverableDevices);
                    for (DeviceIdentity di : discoveredDevices)
                    {
                        if (m_devicesToIgnore.containsKey(di))
                        {
                            continue;
                        }

                        devicesToReport.add(di);
                    }
                    devicesToReport.sort(DeviceIdentity::compareTo);

                    try
                    {
                        await(analyzeDeviceObjects(logger, holder_protocol, devicesToReport));
                    }
                    catch (Throwable t)
                    {
                        logger.warn("Failed to publish device list to Hub: %s", t);
                    }
                }
            }

            return wrapAsync(true);
        }
    }

    private CompletableFuture<Void> checkSubnetTarget(Set<DeviceIdentity> deviceIdentities,
                                                      InetAddress address,
                                                      TimeoutSpec timeSpec) throws
                                                                            Exception
    {
        BACnetManager manager = getManager();
        if (manager != null)
        {
            UdpTransportAddress ta = new UdpTransportAddress(address.getHostAddress(), m_udpNetworkPort);

            DeviceIdentity di = await(manager.probeTarget(LoggerInstance, ta, null, 0, timeSpec));
            if (di != null)
            {
                if (deviceIdentities.add(di))
                {
                    LoggerInstance.info("Subnet scan found %s", di);
                }
            }
        }

        return wrapAsync(null);
    }

    private CompletableFuture<Void> checkAddressBindings(ILogger logger,
                                                         Set<DeviceIdentity> deviceIdentities,
                                                         Set<BACnetDeviceAddress> probedAddressBindinds,
                                                         DeviceIdentity di,
                                                         TimeoutSpec timeSpec) throws
                                                                               Exception
    {
        AsyncWaitMultiple waiter = new AsyncWaitMultiple();

        List<BACnetAddressBinding> lst = await(di.getDeviceAddressBindings(timeSpec));
        if (lst != null)
        {
            for (BACnetAddressBinding addressBinding : lst)
            {
                waiter.add(checkAddressBinding(logger, deviceIdentities, probedAddressBindinds, di, timeSpec, addressBinding));

                await(sleep(20, TimeUnit.MILLISECONDS));
            }
        }

        return waiter.drain();
    }

    private CompletableFuture<Void> checkAddressBinding(ILogger logger,
                                                        Set<DeviceIdentity> deviceIdentities,
                                                        Set<BACnetDeviceAddress> seen,
                                                        DeviceIdentity di,
                                                        TimeoutSpec timeSpec,
                                                        BACnetAddressBinding addressBinding) throws
                                                                                             Exception
    {
        BACnetAddress mac = addressBinding.device_address;
        if (mac != null && mac.couldBeMstp())
        {
            BACnetDeviceAddress deviceAddress = new BACnetDeviceAddress(mac.network_number.unboxUnsigned(), addressBinding.device_identifier.instance_number.unbox());

            if (seen.add(deviceAddress))
            {
                BACnetManager manager = getManager();
                if (manager != null)
                {
                    DeviceIdentity diSub = await(manager.probeTarget(logger, di.getTransportAddress(), mac, deviceAddress, timeSpec));

                    if (diSub != null && deviceIdentities.add(diSub))
                    {
                        LoggerInstance.info("Found MS/TP '%s' through address binding on '%s'", diSub, di);
                    }
                    else if (diSub == null)
                    {
                        LoggerInstance.info("Ignoring MS/TP '%s' from address binding (%s %s), not responding.", deviceAddress, di.getTransportAddress(), mac);
                    }
                }
            }
        }

        return AsyncRuntime.NullResult;
    }

    private static Collection<NetworkHelper.InetAddressWithPrefix> parseFilters(ProtocolConfigForBACnet cfg) throws
                                                                                                             UnknownHostException
    {
        if (cfg.filterSubnets == null || cfg.filterSubnets.isEmpty())
        {
            return null;
        }

        Map<String, NetworkHelper.InetAddressWithPrefix> filters = Maps.newHashMap();

        for (FilteredSubnet filterSubnet : cfg.filterSubnets)
        {
            filters.put(filterSubnet.cidr, NetworkHelper.InetAddressWithPrefix.parse(filterSubnet.cidr));
        }

        return filters.values();
    }

    private static boolean isIncludedInFilters(Collection<NetworkHelper.InetAddressWithPrefix> filters,
                                               DeviceIdentity di)
    {

        UdpTransportAddress udpTa = Reflection.as(di.getTransportAddress(), UdpTransportAddress.class);
        if (udpTa != null)
        {
            InetAddress address = udpTa.socketAddress.getAddress();

            for (NetworkHelper.InetAddressWithPrefix filter : filters)
            {
                if (filter.isInSubnet(address))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private CompletableFuture<Void> analyzeDeviceObjects(ILogger logger,
                                                         GatewayState.ResultHolder holder_protocol,
                                                         List<DeviceIdentity> devices) throws
                                                                                       Exception
    {
        try (ProgressStatusHolder holder_progress = new ProgressStatusHolder("analyzeDeviceObjects", 30, (stats, last, elapsed) ->
        {
            if (last)
            {
                logger.info("Completed Device Object Analysis in %d secs (%d devices, %d objects, %d properties)", elapsed, stats.countDevicesProcessed, stats.countObjects, stats.countProperties);
            }
            else
            {
                logger.info("Device Object Analysis - %d secs (%d total devices, %d devices, %d objects, %d properties so far)",
                            elapsed,
                            stats.countDevicesToProcess,
                            stats.countDevicesProcessed,
                            stats.countObjects,
                            stats.countProperties);
            }
        }))
        {
            AsyncWaitMultiple           waiter = new AsyncWaitMultiple();
            GatewayState.ProgressStatus stats  = holder_progress.get();

            for (DeviceIdentity di : devices)
            {
                synchronized (stats)
                {
                    stats.countDevicesToProcess++;
                }

                waiter.add(analyzeDeviceObject(logger, holder_protocol, di, stats));
            }

            await(waiter.drain());

            return wrapAsync(null);
        }
    }

    private CompletableFuture<Void> analyzeDeviceObject(ILogger logger,
                                                        GatewayState.ResultHolder holder_protocol,
                                                        DeviceIdentity di,
                                                        GatewayState.ProgressStatus stats) throws
                                                                                           Exception
    {
        // Skip unreachable devices.
        boolean isReachable = await(di.isReachable(logger, null, false));
        if (isReachable)
        {
            GatewayState.ResultHolder holder_device = holder_protocol.prepareResult(GatewayDiscoveryEntitySelector.BACnet_Device, di.extractIdentifier(), true);

            final TransportAddress transportAddress = di.getTransportAddress();
            String                 contents         = transportAddress != null ? transportAddress.toString() : null;

            holder_device.queueContents(contents);

            await(analyzeObject(logger, holder_device, di, di.getObjectDescriptor(), false, stats));
        }

        synchronized (stats)
        {
            stats.countDevicesProcessed++;
        }

        return wrapAsync(null);
    }

    //--//

    @Override
    CompletableFuture<Boolean> listObjects(GatewayOperationTracker.State operationContext,
                                           GatewayState.ResultHolder holder_protocol,
                                           GatewayDiscoveryEntity en_protocol) throws
                                                                               Exception
    {
        ILogger logger = operationContext.getContextualLogger(LoggerInstance);

        await(analyzeDevices(logger, holder_protocol, en_protocol, false, false));

        return wrapAsync(true);
    }

    @Override
    CompletableFuture<Boolean> readAllValues(GatewayOperationTracker.State operationContext,
                                             GatewayState.ResultHolder holder_protocol,
                                             GatewayDiscoveryEntity en_protocol) throws
                                                                                 Exception
    {
        ILogger logger = operationContext.getContextualLogger(LoggerInstance);

        await(analyzeDevices(logger, holder_protocol, en_protocol, true, false));

        return wrapAsync(true);
    }

    @Override
    CompletableFuture<Boolean> writeValues(GatewayOperationTracker.State operationContext,
                                           GatewayState.ResultHolder holder_protocol,
                                           GatewayDiscoveryEntity en_protocol) throws
                                                                               Exception
    {
        ILogger logger = operationContext.getContextualLogger(LoggerInstance);

        await(analyzeDevices(logger, holder_protocol, en_protocol, false, true));

        return wrapAsync(true);
    }

    private CompletableFuture<Void> analyzeDevices(ILogger logger,
                                                   GatewayState.ResultHolder holder_protocol,
                                                   GatewayDiscoveryEntity en_protocol,
                                                   boolean readContents,
                                                   boolean writeContents) throws
                                                                          Exception
    {
        BACnetManager manager = getManager();
        if (manager == null)
        {
            return wrapAsync(null);
        }

        if (en_protocol.subEntities == null)
        {
            for (DeviceIdentity di : manager.getDevices(true))
            {
                en_protocol.createAsRequest(GatewayDiscoveryEntitySelector.BACnet_Device, di.extractIdentifier());
            }
        }

        String opText;

        if (readContents)
        {
            opText = "reading properties";
        }
        else if (writeContents)
        {
            opText = "writing properties";
        }
        else
        {
            opText = "listing objects";
        }

        try (ProgressStatusHolder holder_progress = new ProgressStatusHolder("analyzeDevices", 30, (stats, last, elapsed) ->
        {
            if (last)
            {
                logger.info("Completed Device Analysis for %s in %d secs (%d devices, %d objects, %d properties)",
                            opText,
                            elapsed,
                            stats.countDevicesProcessed,
                            stats.countObjects,
                            stats.countProperties);
            }
            else
            {
                logger.info("Device Analysis for %s - %d secs (%d total devices, %d devices, %d objects, %d properties so far)",
                            opText,
                            elapsed,
                            stats.countDevicesToProcess,
                            stats.countDevicesProcessed,
                            stats.countObjects,
                            stats.countProperties);
            }
        }))
        {
            AsyncWaitMultiple           waiter = new AsyncWaitMultiple();
            GatewayState.ProgressStatus stats  = holder_progress.get();

            for (GatewayDiscoveryEntity en_device : en_protocol.filter(GatewayDiscoveryEntitySelector.BACnet_Device))
            {
                BACnetDeviceDescriptor desc = en_device.getSelectorValueAsObject(BACnetDeviceDescriptor.class);
                if (desc == null || desc.address == null)
                {
                    continue;
                }

                DeviceIdentity di = await(manager.resolveDeviceInstance(desc.address, desc.transport, 10, TimeUnit.SECONDS));
                if (di != null)
                {
                    if (m_devicesToIgnore.containsKey(di))
                    {
                        logger.info("Skipping ignore device %s...", di);
                        continue;
                    }

                    synchronized (stats)
                    {
                        stats.countDevicesToProcess++;
                    }

                    waiter.add(analyzeDevice(logger, holder_protocol, desc, en_device, opText, readContents, writeContents, di, stats));
                }
            }

            await(waiter.drain());

            return wrapAsync(null);
        }
    }

    private CompletableFuture<Void> analyzeDevice(ILogger logger,
                                                  GatewayState.ResultHolder holder_protocol,
                                                  BACnetDeviceDescriptor desc,
                                                  GatewayDiscoveryEntity en_device,
                                                  String opText,
                                                  boolean readContents,
                                                  boolean writeContents,
                                                  DeviceIdentity di,
                                                  GatewayState.ProgressStatus stats) throws
                                                                                     Exception
    {
        GatewayState.ResultHolder holder_device = holder_protocol.prepareResult(GatewayDiscoveryEntitySelector.BACnet_Device, desc, true);

        logger.info("Analyzing device [%s] for %s...", di, opText);

        int objectsAnalyzed = 0;

        try
        {
            TreeMap<BACnetObjectIdentifier, DeviceIdentity.ObjectDescriptor> objectDescriptors = await(di.getObjects());

            if (writeContents)
            {
                for (GatewayDiscoveryEntity en_object : en_device.filter(GatewayDiscoveryEntitySelector.BACnet_ObjectSet))
                {
                    BACnetObjectIdentifier objId = new BACnetObjectIdentifier(en_object.selectorValue);

                    DeviceIdentity.ObjectDescriptor objDesc = objectDescriptors.get(objId);
                    if (objDesc != null)
                    {
                        BACnetObjectModel obj = en_object.getContentsAsObject(BACnetObjectModel.getObjectMapper(), new TypeReference<>()
                        {
                        });

                        for (BACnetPropertyIdentifierOrUnknown prop : obj.getAccessedProperties())
                        {
                            await(di.writeProperty(getManager().getDefaultTimeout(), objDesc.id, prop, obj.getValue(prop, null)));
                        }

                        await(analyzeObject(logger, holder_device, di, objDesc, true, stats));
                        objectsAnalyzed++;
                    }
                }
            }
            else
            {

                if (en_device.subEntities != null)
                {
                    for (GatewayDiscoveryEntity en_object : en_device.filter(GatewayDiscoveryEntitySelector.BACnet_ObjectConfig))
                    {
                        BACnetObjectIdentifier objId = new BACnetObjectIdentifier(en_object.selectorValue);

                        DeviceIdentity.ObjectDescriptor objDesc = objectDescriptors.get(objId);
                        if (objDesc != null)
                        {
                            await(analyzeObject(logger, holder_device, di, objDesc, readContents, stats));
                            objectsAnalyzed++;
                        }
                    }
                }
                else
                {
                    for (DeviceIdentity.ObjectDescriptor objDesc : objectDescriptors.values())
                    {
                        await(analyzeObject(logger, holder_device, di, objDesc, readContents, stats));
                        objectsAnalyzed++;
                    }
                }
            }

            di.flushObjects();

            logger.info("Analyzed device [%s], found %d objects", di, objectsAnalyzed);
        }
        catch (Throwable t)
        {
            if (!BACnetManager.uniqueTraces && t instanceof TimeoutException)
            {
                logger.info("Analyzed device [%s], found %d objects (but aborted due to timeout)", di, objectsAnalyzed);
            }
            else
            {
                logger.info("Analyzed device [%s], found %d objects (but aborted due to %s)", di, objectsAnalyzed, t);
            }
        }

        synchronized (stats)
        {
            stats.countDevicesProcessed++;
        }

        m_gatewayState.startFlushingOfEntities(false);

        return wrapAsync(null);
    }

    private CompletableFuture<Void> analyzeObject(ILogger logger,
                                                  GatewayState.ResultHolder holder_device,
                                                  DeviceIdentity di,
                                                  DeviceIdentity.ObjectDescriptor objDesc,
                                                  boolean readContents,
                                                  GatewayState.ProgressStatus stats)
    {
        logger.debug("Analyzing object %s...", objDesc);

        String contents = null;

        recordSamplingTarget(di);

        if (readContents && !objDesc.id.object_type.isUnknown()) // We don't read proprietary objects
        {
            try
            {
                for (int retry = 0; retry < 4; retry++)
                {
                    DeviceIdentity.ObjectReadResult result = await(readAllProperties(di, objDesc, stats));

                    if (result != null)
                    {
                        contents = result.state.serializeToJson();
                        break;
                    }
                }
            }
            catch (Throwable t)
            {
                logger.info("Failed to read contents of object %s on device [%s], due to %s", objDesc, di, t);
            }
        }

        GatewayState.ResultHolder holder_object = holder_device.prepareResult(GatewayDiscoveryEntitySelector.BACnet_Object, objDesc.id.toJsonValue(), true);
        holder_object.queueContents(contents);

        synchronized (stats)
        {
            stats.countObjects++;
        }

        return wrapAsync(null);
    }

    //--//

    @Override
    CompletableFuture<Void> startSamplingConfiguration(GatewayOperationTracker.State operationContext) throws
                                                                                                       Exception
    {
        ILogger logger = operationContext.getContextualLogger(LoggerInstance);

        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            logger.info("Started configuring sampling for objects on network '%s'", m_configuration.name);

            m_samplingStateNext = new SamplingConfig<>();

            ProtocolConfigForBACnet cfg = m_configuration.getProtocolConfiguration(ProtocolConfigForBACnet.class);
            cfg.samplingConfigurationId = null;

            return wrapAsync(null);
        }
    }

    @Override
    CompletableFuture<Void> updateSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                        GatewayDiscoveryEntity en_protocol) throws
                                                                                            Exception
    {
        ILogger logger = operationContext.getContextualLogger(LoggerInstance);

        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            if (m_samplingStateNext != null)
            {
                BACnetManager manager = getManager();
                if (manager != null)
                {
                    for (GatewayDiscoveryEntity en_device : en_protocol.filter(GatewayDiscoveryEntitySelector.BACnet_Device))
                    {
                        BACnetDeviceDescriptor desc = en_device.getSelectorValueAsObject(BACnetDeviceDescriptor.class);
                        if (desc == null || desc.address == null)
                        {
                            continue;
                        }

                        DeviceIdentity di = manager.registerDevice(desc);
                        if (di != null)
                        {
                            di.startResolution();

                            if (m_devicesToIgnore.containsKey(di))
                            {
                                logger.info("Skipping ignore device [%s]...", di);
                                continue;
                            }

                            await(configureDevice(logger, m_samplingStateNext, en_device, di));
                        }
                    }
                }
            }

            return wrapAsync(null);
        }
    }

    @Override
    CompletableFuture<Void> completeSamplingConfiguration(GatewayOperationTracker.State operationContext,
                                                          String configId) throws
                                                                           Exception
    {
        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            ILogger logger = operationContext.getContextualLogger(LoggerInstance);

            completeSamplingConfiguration(logger, true, configId);

            return wrapAsync(null);
        }
    }

    private void completeSamplingConfiguration(ILogger logger,
                                               boolean saveConfiguration,
                                               String configId) throws
                                                                Exception
    {
        SamplingConfig<DeviceIdentity, BACnetObjectIdentifier, BACnetPropertyIdentifierOrUnknown> samplingState = m_samplingStateNext;

        SamplingEntries configToSave = saveConfiguration ? new SamplingEntries() : null;
        if (configToSave != null && samplingState != null)
        {
            samplingState.enumerate(-1, (period, deviceId, objId, prop) ->
            {
                SamplingEntry entry = new SamplingEntry();
                entry.period   = period;
                entry.deviceId = deviceId.extractIdentifier();
                entry.objId    = objId;
                entry.prop     = prop;
                configToSave.entries.add(entry);
            });
        }

        m_samplingTargets.clear();
        m_samplingState     = samplingState;
        m_samplingStateNext = null;

        if (samplingState != null)
        {
            queueNextSampling(samplingState, 1, -1);

            logger.info("Done configuring sampling for objects on network '%s': %d devices, %d objects, %d properties",
                        m_configuration.name,
                        samplingState.stats.countDevices,
                        samplingState.stats.countObjects,
                        samplingState.stats.countProperties);
        }

        if (configToSave != null)
        {
            ProtocolConfigForBACnet cfg = m_configuration.getProtocolConfiguration(ProtocolConfigForBACnet.class);
            cfg.samplingConfigurationId = configId;
            m_gatewayState.saveConfiguration(m_configuration, cfg, (output) -> ObjectMappers.SkipNulls.writeValue(output, configToSave));
        }
    }

    private void queueNextSampling(SamplingConfig<DeviceIdentity, BACnetObjectIdentifier, BACnetPropertyIdentifierOrUnknown> samplingState,
                                   int sequenceNumber,
                                   int selectPeriod)
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

                    executeSampling(samplingState, nextSample, sequenceNumber, period, diffMilliseconds, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    @AsyncBackground
    private CompletableFuture<Void> executeSampling(SamplingConfig<DeviceIdentity, BACnetObjectIdentifier, BACnetPropertyIdentifierOrUnknown> samplingState,
                                                    long samplingSlot,
                                                    int sequenceNumber,
                                                    int period,
                                                    @AsyncDelay long delay,
                                                    @AsyncDelay TimeUnit delayUnit)
    {
        BACnetManager currentManager = getManager();

        if (currentManager == null || samplingState != m_samplingState)
        {
            // Something reset the timer, bail out.
            return wrapAsync(null);
        }

        String sampleContext = String.format("[Period %3d / Seq %7d / %s]", period, sequenceNumber, m_configuration.namePadded);

        var contextualLogger = new RedirectingLogger(LoggerInstanceForProgress)
        {
            @Override
            public String getPrefix()
            {
                return sampleContext;
            }
        };

        try (ProgressStatusHolder holder_progress = new ProgressStatusHolder(null, 30, (stats, last, elapsed) ->
        {
            if (last)
            {
                reportSamplingDone(sequenceNumber, m_configuration.namePadded, samplingSlot, period, stats);

                if (stats.hasAnyFailure())
                {
                    if (stats.countDevicesUnreachable > 0)
                    {
                        contextualLogger.debug("Sampling end  : %d unreachable devices, %d objects, %d properties, %d failures => %d timeouts, %d deadlines, %d unknown objs/props",
                                               stats.countDevicesUnreachable,
                                               stats.countObjects,
                                               stats.countProperties,
                                               stats.countPropertiesBad,
                                               stats.countTimeouts,
                                               stats.countDeadlines,
                                               stats.countUnknowns);
                    }
                    else
                    {
                        contextualLogger.debug("Sampling end  : %d objects, %d properties, %d failures => %d timeouts, %d deadlines, %d unknown objs/props",
                                               stats.countObjects,
                                               stats.countProperties,
                                               stats.countPropertiesBad,
                                               stats.countTimeouts,
                                               stats.countDeadlines,
                                               stats.countUnknowns);
                    }
                }
                else
                {
                    contextualLogger.debug("Sampling end  : %d objects, %d properties", stats.countObjects, stats.countProperties);
                }
            }
            else
            {
                if (stats.countDevicesUnreachable > 0)
                {
                    contextualLogger.debugVerbose("Sampling      : heartbeat %d secs (%d unreachable devices, %d devices, %d objects, %d properties so far)",
                                                  elapsed,
                                                  stats.countDevicesUnreachable,
                                                  stats.countDevicesProcessed,
                                                  stats.countObjects,
                                                  stats.countProperties);
                }
                else
                {
                    contextualLogger.debugVerbose("Sampling      : heartbeat %d secs (%d devices, %d objects, %d properties so far)",
                                                  elapsed,
                                                  stats.countDevicesProcessed,
                                                  stats.countObjects,
                                                  stats.countProperties);
                }
            }
        }))
        {
            Map<DeviceIdentity, DeviceIdentity.BatchReader> batches = Maps.newHashMap();
            GatewayState.ProgressStatus                     stats   = holder_progress.get();

            MonotonousTime deadline = TimeUtils.computeTimeoutExpiration(period, TimeUnit.SECONDS)
                                               .minus(500, ChronoUnit.MILLIS);

            contextualLogger.debug("Sampling start:");

            try
            {
                samplingState.prepareBatches(samplingSlot, period, (di, objId, prop) ->
                {
                    DeviceIdentity.ObjectDescriptor objDesc = di.ensureObjectDescriptor(objId);
                    if (objDesc != null && objDesc.getFailureCount(prop) < MAX_FAILURES_BEFORE_DISABLING)
                    {
                        DeviceIdentity.BatchReader reader = batches.computeIfAbsent(di, DeviceIdentity::createBatchReader);

                        reader.add(objDesc, prop);
                    }
                });

                samplingState.doneWithCurrentSample(period);

                queueNextSampling(samplingState, sequenceNumber + 1, period);

                //--//

                GatewayState.ResultHolder holder_root     = m_gatewayState.getRoot(samplingSlot);
                GatewayState.ResultHolder holder_network  = holder_root.prepareResult(GatewayDiscoveryEntitySelector.Network, m_configuration.sysId, false);
                GatewayState.ResultHolder holder_protocol = holder_network.prepareResult(GatewayDiscoveryEntitySelector.Protocol, GatewayDiscoveryEntity.Protocol_BACnet, false);

                AsyncWaitMultiple waiter = new AsyncWaitMultiple();

                for (DeviceIdentity di : batches.keySet())
                {
                    DeviceIdentity.BatchReader reader = batches.get(di);

                    // Execute all the reads in parallel.
                    waiter.add(executeBatch(contextualLogger, currentManager, di, reader, holder_protocol, deadline, stats));
                }

                // Release all the references to the batches, to reduce memory pressure.
                batches.clear();

                await(waiter.drain());
            }
            catch (Exception e)
            {
                contextualLogger.error("Encountered a problem while sampling: %s", e);
            }
        }

        m_gatewayState.startFlushingOfEntities(false);

        return wrapAsync(null);
    }

    private CompletableFuture<Void> executeBatch(ILogger contextualLogger,
                                                 BACnetManager currentManager,
                                                 DeviceIdentity di,
                                                 DeviceIdentity.BatchReader reader,
                                                 GatewayState.ResultHolder holder_protocol,
                                                 MonotonousTime deadline,
                                                 GatewayState.ProgressStatus stats) throws
                                                                                    Exception
    {
        // Retry a few times, but only wait at most twice as long the maximum response time.
        TimeoutSpec responseTime = di.getEstimatedResponseTime(8, 2)
                                     .withDeadline(deadline);

        DeviceIdentity.BatchReaderResult output      = await(reader.execute(responseTime));
        BatchReaderStats                 outputStats = new BatchReaderStats();

        if (currentManager.isClosed())
        {
            // Manager got stop during sampling, ignore all results.
            return wrapAsync(null);
        }

        try (AsyncMutex.Holder holder = await(m_mutex.acquire()))
        {
            GatewayState.ResultHolder holder_device = holder_protocol.prepareResult(GatewayDiscoveryEntitySelector.BACnet_Device, di.extractIdentifier(), false);

            recordSamplingTarget(di);

            for (DeviceIdentity.ObjectDescriptor objDesc : output.values.keySet())
            {
                outputStats.countObjects++;

                DeviceIdentity.ObjectReadResult result               = output.values.get(objDesc);
                boolean                         gotSomeObjectResults = false;

                for (BACnetPropertyIdentifierOrUnknown prop : result.getProperties())
                {
                    outputStats.countProperties++;

                    BACnetObjectModel.FailureDetails failure = result.getFailureDetails(prop);

                    if (failure == null)
                    {
                        objDesc.resetFailureCount(prop);
                        gotSomeObjectResults = true;
                    }
                    else
                    {
                        outputStats.countPropertiesBad++;
                        boolean logFailure = false;

                        if (failure.error != null)
                        {
                            switch (failure.error.error_code)
                            {
                                case unknown_device:
                                case unknown_object:
                                case unsupported_object_type:
                                case no_objects_of_specified_type:
                                case unknown_property:
                                case value_not_initialized:
                                    contextualLogger.debug("Sampling of %s/%s on [%s] failed due to %s/%s...", objDesc.id, prop, di, failure.error.error_class, failure.error.error_code);

                                    int count = objDesc.incrementFailureCount(prop);
                                    if (count == MAX_FAILURES_BEFORE_DISABLING)
                                    {
                                        contextualLogger.debug("Sampling of %s/%s on [%s] failed too many times, disabling...", objDesc.id, prop, di);
                                    }

                                    outputStats.countUnknowns++;
                                    break;

                                default:
                                    logFailure = true;
                                    break;
                            }
                        }
                        else if (failure.deadline != null)
                        {
                            outputStats.countDeadlines++;
                        }
                        else if (failure.timeout)
                        {
                            outputStats.countTimeouts++;
                        }

                        if (logFailure)
                        {
                            BatchReaderFailureDetails details = new BatchReaderFailureDetails();
                            details.id      = objDesc.id;
                            details.prop    = prop;
                            details.failure = failure.asException(objDesc.id, prop);

                            if (outputStats.failures == null)
                            {
                                outputStats.failures = HashMultimap.create();
                            }

                            outputStats.failures.put(failure.getClass(), details);
                        }
                    }
                }

                contextualLogger.debugObnoxious("Sampling of %s: %s", objDesc.id, result.getProperties());

                GatewayState.ResultHolder holder_object = holder_device.prepareResult(GatewayDiscoveryEntitySelector.BACnet_Object, objDesc.id.toJsonValue(), false);

                // Even if all the properties failed to read, we create a empty sample, so we know something was not right.
                String contents = gotSomeObjectResults ? result.state.serializeToJson() : null;

                GatewayState.ResultHolder holder_sample = holder_object.prepareResult(GatewayDiscoveryEntitySelector.BACnet_ObjectSample, (String) null, true);
                holder_sample.queueContents(contents);
            }

            if (outputStats.countProperties == outputStats.countPropertiesBad)
            {
                stats.countDevicesUnreachable++;

                di.markAsUnreachable();
            }
            else
            {
                di.markAsReachable();
            }

            di.reportReachabilityChange((isReachable, lastReachable) ->
                                        {
                                            if (!isReachable)
                                            {
                                                contextualLogger.warn("Sampling for '%s' failed to read any property: %d objects and %d properties with %s",
                                                                      di,
                                                                      outputStats.countObjects,
                                                                      outputStats.countProperties,
                                                                      output);
                                            }

                                            GatewayState.ResultHolder holder_deviceUnreachable = holder_device.prepareResult(GatewayDiscoveryEntitySelector.BACnet_Reachability, (String) null, true);

                                            DeviceReachability report = new DeviceReachability();
                                            report.reachable     = isReachable;
                                            report.lastReachable = lastReachable;

                                            holder_deviceUnreachable.queueContents(ObjectMappers.SkipNulls.writeValueAsString(report));
                                            return true;
                                        });

            if (contextualLogger.isEnabled(Severity.DebugVerbose))
            {
                contextualLogger.debugVerbose("Sampling for '%s' processed %d objects and %d properties (%d failures, %d timeouts, %d deadlines, %d unknowns) with %s",
                                              di,
                                              outputStats.countObjects,
                                              outputStats.countProperties,
                                              outputStats.countPropertiesBad,
                                              outputStats.countTimeouts,
                                              outputStats.countDeadlines,
                                              outputStats.countUnknowns,
                                              output);
            }

            stats.countObjects += outputStats.countObjects;
            stats.countProperties += outputStats.countProperties;
            stats.countPropertiesBad += outputStats.countPropertiesBad;
            stats.countUnknowns += outputStats.countUnknowns;
            stats.countDeadlines += outputStats.countDeadlines;
            stats.countTimeouts += outputStats.countTimeouts;

            if (outputStats.failures != null)
            {
                for (Class<?> clz : outputStats.failures.keySet())
                {
                    Collection<BatchReaderFailureDetails> coll = outputStats.failures.get(clz);

                    StringBuilder sb    = new StringBuilder();
                    int           count = 0;

                    for (BatchReaderFailureDetails details : coll)
                    {
                        if (sb.length() > 0)
                        {
                            sb.append(", ");
                        }

                        if (count >= 10)
                        {
                            sb.append(String.format("<%d more failures>", coll.size() - count));
                            break;
                        }

                        sb.append(String.format("%s/%s", details.id, details.prop));
                        count++;
                    }

                    if (clz == TimeoutException.class)
                    {
                        contextualLogger.warn("Timeout reading for '%s': %s", di, sb);
                    }
                    else
                    {
                        // Just print the stack trace for the first one.
                        BatchReaderFailureDetails details = CollectionUtils.firstElement(coll);

                        if (contextualLogger.isEnabled(Severity.DebugVerbose))
                        {
                            contextualLogger.debugVerbose("Failure reading for '%s' on %s: %s", di, sb, details.failure);
                        }
                        else
                        {
                            contextualLogger.warn("Failure reading for '%s' on %s: %s", di, sb, details.failure.getMessage());
                        }
                    }
                }
            }

            stats.countDevicesProcessed++;
        }

        m_gatewayState.startFlushingOfEntities(false);

        return wrapAsync(null);
    }

    private CompletableFuture<Void> configureDevice(ILogger logger,
                                                    SamplingConfig<DeviceIdentity, BACnetObjectIdentifier, BACnetPropertyIdentifierOrUnknown> samplingState,
                                                    GatewayDiscoveryEntity en_device,
                                                    DeviceIdentity di)
    {
        logger.debug("Configuring device [%s]...", di);

        BACnetManager manager = getManager();
        if (manager != null)
        {
            Function<Set<BACnetPropertyIdentifierOrUnknown>, Set<BACnetPropertyIdentifierOrUnknown>> memoizingCallback = manager::memoizeProperties;

            int numBefore = samplingState.stats.countObjects;

            for (GatewayDiscoveryEntity en_object : en_device.filter(GatewayDiscoveryEntitySelector.BACnet_ObjectConfig))
            {
                BACnetObjectIdentifier objId = new BACnetObjectIdentifier(en_object.selectorValue);

                try
                {
                    logger.debug("Configuring object %s...", objId);

                    DeviceIdentity.ObjectDescriptor                      objDesc   = di.ensureObjectDescriptor(objId);
                    Multimap<Integer, BACnetPropertyIdentifierOrUnknown> perPeriod = ArrayListMultimap.create();

                    Map<String, Integer> map = en_object.getContentsForObjectConfig(ObjectMappers.SkipNulls);
                    for (String prop : map.keySet())
                    {
                        BACnetPropertyIdentifierOrUnknown propId = BACnetPropertyIdentifierOrUnknown.parse(prop);

                        //
                        // To avoid reloading all the schema from the object, use the metadata from its class.
                        // This might cause read failures for unsupported properties.
                        // We'll simply fix them up (i.e. remove them from the configuration) when we perform a sampling pass.
                        //
                        if (objDesc != null && !objDesc.hasKnownProperty(propId))
                        {
                            continue;
                        }

                        final int period = map.get(prop);
                        perPeriod.put(period, propId);
                    }

                    for (Integer period : perPeriod.keySet())
                    {
                        samplingState.add(period, di, objId, perPeriod.get(period), memoizingCallback);
                    }
                }
                catch (Exception e)
                {
                    logger.warn("Failed to configure object %s, due to exception: %s", objId, e);
                }
            }

            int numAfter = samplingState.stats.countObjects;

            logger.debug("Configured device [%s], Gateway will sample %d objects", di, numAfter - numBefore);

            if (samplingState.stats.shouldReport())
            {
                logger.info("Configuring sampling: %d devices, %d objects, %d properties so far...",
                            samplingState.stats.countDevices,
                            samplingState.stats.countObjects,
                            samplingState.stats.countProperties);
            }
        }

        return wrapAsync(null);
    }

    //--//

    private CompletableFuture<DeviceIdentity.ObjectReadResult> readAllProperties(DeviceIdentity di,
                                                                                 DeviceIdentity.ObjectDescriptor objDesc,
                                                                                 GatewayState.ProgressStatus stats) throws
                                                                                                                    Exception
    {
        Set<BACnetPropertyIdentifierOrUnknown> set = await(objDesc.getProperties());

        DeviceIdentity.BatchReader reader = di.createBatchReader();

        for (BACnetPropertyIdentifierOrUnknown prop : set)
        {
            reader.add(objDesc, prop);
        }

        stats.countProperties += set.size();

        BACnetManager                    manager = getManager();
        DeviceIdentity.BatchReaderResult output  = await(reader.execute(manager.getDefaultTimeout()));

        return wrapAsync(output.values.get(objDesc));
    }

    private void recordSamplingTarget(DeviceIdentity di)
    {
        TransportAddress ta = di.getTransportAddress();
        if (ta != null)
        {
            m_samplingTargets.add(ta);
        }
    }
}