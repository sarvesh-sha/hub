/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import javax.validation.constraints.NotNull;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.collection.WeakLinkedList;
import com.optio3.concurrency.AsyncMutex;
import com.optio3.concurrency.AsyncSemaphore;
import com.optio3.concurrency.AsyncWaitMultiple;
import com.optio3.infra.NetworkHelper;
import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned8;
import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.logging.RedirectingLogger;
import com.optio3.logging.Severity;
import com.optio3.protocol.bacnet.model.enums.MessageType;
import com.optio3.protocol.bacnet.model.enums.NetworkPriority;
import com.optio3.protocol.bacnet.model.linklayer.BaseVirtualLinkLayer;
import com.optio3.protocol.bacnet.model.linklayer.ForeignDeviceTableEntry;
import com.optio3.protocol.bacnet.model.pdu.NetworkPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ConfirmedRequestPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ServiceCommon;
import com.optio3.protocol.bacnet.model.pdu.application.UnconfirmedRequestPDU;
import com.optio3.protocol.bacnet.model.pdu.network.IAmRouterToNetworkPDU;
import com.optio3.protocol.bacnet.model.pdu.network.NetworkMessagePDU;
import com.optio3.protocol.bacnet.model.pdu.network.WhoIsRouterToNetworkPDU;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.protocol.bacnet.model.pdu.request.UnconfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ReadProperty;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ReadPropertyMultiple;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.IAm;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.WhoIs;
import com.optio3.protocol.bacnet.transport.AbstractTransport;
import com.optio3.protocol.bacnet.transport.UdpTransport;
import com.optio3.protocol.model.TransportNetworkDescriptor;
import com.optio3.protocol.model.TransportPerformanceCounters;
import com.optio3.protocol.model.bacnet.BACnetAddress;
import com.optio3.protocol.model.bacnet.BACnetDeviceAddress;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.constructed.ReadAccessResult;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetSegmentation;
import com.optio3.protocol.model.bacnet.objects.device;
import com.optio3.protocol.model.config.WhoIsRange;
import com.optio3.protocol.model.transport.TransportAddress;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;

public final class BACnetManager extends RedirectingLogger implements AutoCloseable
{
    static final class NetworkState
    {
        private final static int c_adjustmentFrequency = 20; // Every X packets.

        final TransportPerformanceCounters statistics = new TransportPerformanceCounters();
        final AsyncSemaphore               concurrentAccesses;

        int m_packetsSuccess;
        int m_packetsTimeout;

        NetworkState(int maxParallelRequests)
        {
            concurrentAccesses = new AsyncSemaphore(maxParallelRequests);
        }

        void recordNetworkRequestOutcome(DeviceIdentity target,
                                         boolean success,
                                         boolean timeout)
        {
            if (success)
            {
                m_packetsSuccess++;
            }

            if (timeout)
            {
                m_packetsTimeout++;
            }

            final int totalPackets = m_packetsSuccess + m_packetsTimeout;
            if (totalPackets >= c_adjustmentFrequency)
            {
                int timeoutsPercent = 100 * m_packetsTimeout / totalPackets;

                if (timeoutsPercent < 5)
                {
                    if (concurrentAccesses.getBoost() < 0)
                    {
                        concurrentAccesses.addToBoost(1);

                        LoggerInstanceForPermits.debug("[%s] Increase permits, failure rate (%d%%) is below 10%%", target, timeoutsPercent);
                    }
                }
                else if (timeoutsPercent > 15)
                {
                    concurrentAccesses.addToBoost(-1);

                    LoggerInstanceForPermits.debug("[%s] Decrease permits, failure rate (%d%%) is above 30%%", target, timeoutsPercent);
                }

                m_packetsTimeout = 0;
                m_packetsSuccess = 0;
            }
        }
    }

    static class InvokeIdHolder implements AutoCloseable
    {
        private final BitSet m_invokeIdInUse;
        private final int    m_invokeId;

        InvokeIdHolder(BitSet invokeIdInUse,
                       int invokeId)
        {
            m_invokeIdInUse = invokeIdInUse;
            m_invokeId      = invokeId;
        }

        @Override
        public void close()
        {
            synchronized (m_invokeIdInUse)
            {
                m_invokeIdInUse.clear(m_invokeId);
            }
        }

        byte get()
        {
            return (byte) (m_invokeId & 0xFF);
        }
    }

    final class TransportState
    {
        private final TransportAddress m_address;
        private final BitSet           m_invokeIdInUse = new BitSet(256);
        private       int              m_invokeIdCounter;

        final TransportPerformanceCounters statistics = new TransportPerformanceCounters();
        final LinkedList<DeviceIdentity>   devices    = new LinkedList<>();
        final Map<Integer, NetworkState>   networks   = Maps.newHashMap();
        final AsyncSemaphore               concurrentAccesses;

        TransportState(TransportAddress address)
        {
            m_address          = address;
            concurrentAccesses = new AsyncSemaphore(m_maxParallelRequestsPerIP);

            //
            // Some devices are picky about what invokeId they get from run to run,
            // aborting the request if they think they have already seen it "recently".
            // To reduce this chance, pick a random number to start with.
            //
            m_invokeIdCounter = (int) (Math.random() * 255);
        }

        InvokeIdHolder tryToAcquireInvokeId()
        {
            int invokeId;

            synchronized (m_invokeIdInUse)
            {
                invokeId = m_invokeIdCounter;

                if (m_invokeIdInUse.get(invokeId))
                {
                    LoggerInstance.debugVerbose("Transport %s hit a invokeId collision, %d in use...", m_address, m_invokeIdInUse.cardinality());

                    // The next id is busy, fail.
                    return null;
                }

                m_invokeIdInUse.set(invokeId);

                m_invokeIdCounter = (invokeId + 1) % 256;
            }

            return new InvokeIdHolder(m_invokeIdInUse, invokeId);
        }

        NetworkState accessNetworkState(BACnetDeviceAddress address)
        {
            NetworkState state = networks.get(address.networkNumber);
            if (state == null)
            {
                state = new NetworkState(m_maxParallelRequestsPerNetwork);
                networks.put(address.networkNumber, state);
            }

            return state;
        }
    }

    public static class EstimatedPayloadSize
    {
        public int requestSizeOverhead;
        public int requestSize;

        // We don't know how long they'll be, use a placeholder.
        public int responseSize         = -1;
        public int responseSizeOverhead = -1;

        public void updateRequest(BACnetObjectIdentifier objId,
                                  BACnetPropertyIdentifierOrUnknown propId)
        {
            try
            {
                ReadPropertyMultiple req = new ReadPropertyMultiple();

                req.add(objId, propId);
                int size1;

                try (OutputBuffer ob = req.encodePayload())
                {
                    size1 = ob.size();
                }

                req.add(objId, propId);
                int size2;

                try (OutputBuffer ob = req.encodePayload())
                {
                    size2 = ob.size();
                }

                int diff = size2 - size1;
                requestSize         = diff;
                requestSizeOverhead = Math.max(0, size1 - diff);
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Failed to determine request size for %s/%s: %s", objId, propId, t);
                requestSize         = 32;
                requestSizeOverhead = 0;
            }
        }

        public void updateResponse(BACnetObjectIdentifier objId,
                                   BACnetPropertyIdentifierOrUnknown propId,
                                   Object value)
        {
            if (responseSize < 0 && value != null)
            {
                if (value instanceof String)
                {
                    responseSize         = 64; // Assume long strings.
                    responseSizeOverhead = 0;
                    return;
                }

                try
                {
                    ReadPropertyMultiple.Ack res = new ReadPropertyMultiple.Ack();

                    ReadAccessResult.Values value1 = res.add(objId, propId);
                    value1.property_value = value;
                    int size1;

                    try (OutputBuffer ob = res.encodePayload())
                    {
                        size1 = ob.size();
                    }

                    ReadAccessResult.Values value2 = res.add(objId, propId);
                    value2.property_value = value;
                    int size2;

                    try (OutputBuffer ob = res.encodePayload())
                    {
                        size2 = ob.size();
                    }

                    int diff = size2 - size1;
                    responseSize         = diff;
                    responseSizeOverhead = Math.max(0, size1 - diff);
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Failed to determine response size for %s/%s: %s", objId, propId, t);
                    responseSize         = 32;
                    responseSizeOverhead = 0;
                }
            }
        }
    }

    //--//

    public static class Permits
    {
    }

    public static final Logger  LoggerInstance           = new Logger(BACnetManager.class);
    public static final Logger  LoggerInstanceForPermits = LoggerInstance.createSubLogger(Permits.class);
    public static       boolean uniqueTraces;

    public static final int c_DefaultPort = 0xBAC0;

    //--//

    private       Integer                 m_vendorId;
    private       Integer                 m_networkNumber;
    private       Integer                 m_instanceNumber;
    private final List<TransportAddress>  m_BBMDs      = Lists.newArrayList();
    private       List<AbstractTransport> m_transports = Lists.newArrayList();

    private int m_maxParallelRequestsPerIP      = 64;
    private int m_maxParallelRequestsPerNetwork = 8;

    private int         m_defaultRetries = 15;
    private Duration    m_defaultTimeout = Duration.of(1, ChronoUnit.SECONDS);
    private TimeoutSpec m_defaultTimeoutSpec;

    //--//

    private boolean m_started;
    private boolean m_shutdown;

    private final TransportPerformanceCounters m_globalStatistics = new TransportPerformanceCounters();

    // @formatter:off
    private final Map<Class<? extends ServiceCommon       >, LinkedList<ServiceListenerHandle       <? extends ServiceCommon       >>> m_listeners                  = Maps.newHashMap();
    private final Map<Class<? extends BaseVirtualLinkLayer>, LinkedList<LinkLayerListenerHandle     <? extends BaseVirtualLinkLayer>>> m_listenersForLinkLayer      = Maps.newHashMap();
    private final Map<Class<? extends NetworkMessagePDU   >, LinkedList<NetworkMessageListenerHandle<? extends NetworkMessagePDU   >>> m_listenersForNetworkMessage = Maps.newHashMap();

    private final Map<TransportAddress   , TransportState                  >  m_transportState        = Maps.newHashMap();
    private final Map<Integer            , LinkedList    <DeviceIdentity  >>  m_instanceToDevices     = Maps.newHashMap();
    private final Map<BACnetDeviceAddress, AsyncMutex                      >  m_pendingProbes         = Maps.newHashMap();
    private final Map<TransportAddress   , AsyncMutex                      >  m_pendingProbesWildcard = Maps.newHashMap();
    private final                          WeakLinkedList<InvokeIdListener>[] m_invokeIdToListeners;

    private final Multimap<Integer, Set<BACnetPropertyIdentifierOrUnknown>> m_memoizedProperties = ArrayListMultimap.create();
    private final Map<BACnetObjectIdentifier, BACnetObjectIdentifier>       m_memoizedObjectIds  = Maps.newHashMap();

    private final Map<BACnetObjectTypeOrUnknown, Map<BACnetPropertyIdentifierOrUnknown, EstimatedPayloadSize>> m_payloadEstimations = Maps.newHashMap();
    // @formatter:on

    private ServiceListenerHandle<WhoIs> m_listenerWhois;

    public BACnetManager()
    {
        super(LoggerInstance);

        // Generic arrays can't be allocated, so we have to do this dance...
        @SuppressWarnings("unchecked") WeakLinkedList<InvokeIdListener>[] array = new WeakLinkedList[256];
        m_invokeIdToListeners = array;
    }

    //--//

    public synchronized void addTransport(AbstractTransport transport)
    {
        if (!m_transports.contains(transport))
        {
            //
            // Copy list, since published lists are considered immutable.
            //
            List<AbstractTransport> transports = Lists.newArrayList(m_transports);
            transports.add(transport);
            m_transports = transports;
        }
    }

    public synchronized void addBBMD(TransportAddress ta)
    {
        CollectionUtils.addIfMissingAndNotNull(m_BBMDs, ta);
    }

    public Integer getVendorId()
    {
        return m_vendorId;
    }

    public void setVendorId(int num)
    {
        Preconditions.checkArgument(num >= 0);

        m_vendorId = num;
    }

    public Integer getNetworkNumber()
    {
        return BoxingUtils.get(m_networkNumber, BACnetAddress.LocalNetwork);
    }

    public void setNetworkNumber(int num)
    {
        Preconditions.checkArgument(num >= 0 && num < 0xFF_FF);

        m_networkNumber = num;
    }

    public Integer getInstanceNumber()
    {
        return m_instanceNumber;
    }

    public void setInstanceNumber(int num)
    {
        Preconditions.checkArgument(num >= 0 && num < (1 << 20));

        m_instanceNumber = num;
    }

    public int getMaxParallelRequestsPerIP()
    {
        return m_maxParallelRequestsPerIP;
    }

    public void setMaxConcurrencyPerIP(int maxParallelRequestsPerIP)
    {
        Preconditions.checkArgument(maxParallelRequestsPerIP > 0);

        m_maxParallelRequestsPerIP = maxParallelRequestsPerIP;
    }

    public int getMaxParallelRequestsPerNetwork()
    {
        return m_maxParallelRequestsPerNetwork;
    }

    public void setMaxConcurrencyPerNetwork(int maxParallelRequestsPerNetwork)
    {
        Preconditions.checkArgument(maxParallelRequestsPerNetwork > 0);

        m_maxParallelRequestsPerNetwork = maxParallelRequestsPerNetwork;
    }

    public void setDefaultRetries(int retries)
    {
        Preconditions.checkArgument(retries > 0);

        m_defaultRetries     = retries;
        m_defaultTimeoutSpec = null;
    }

    public void setDefaultTimeout(Duration timeout)
    {
        Preconditions.checkNotNull(timeout);

        m_defaultTimeout     = timeout;
        m_defaultTimeoutSpec = null;
    }

    public TimeoutSpec getDefaultTimeout()
    {
        if (m_defaultTimeoutSpec == null)
        {
            m_defaultTimeoutSpec = TimeoutSpec.create(m_defaultRetries, m_defaultTimeout);
        }

        return m_defaultTimeoutSpec;
    }

    //--//

    public void start()
    {
        if (!m_started)
        {
            if (m_listenerWhois == null)
            {
                if (getInstanceNumber() != null && getVendorId() != null)
                {
                    //
                    // Acting like a device, respond to WhoIs requests.
                    //
                    m_listenerWhois = registerListener(WhoIs.class, (msg, sc) ->
                    {
                        IAm res = new IAm();
                        res.i_am_device_identifier   = BACnetObjectType.device.asObjectIdentifier(getInstanceNumber());
                        res.segmentation_supported   = BACnetSegmentation.no_segmentation;
                        res.max_apdu_length_accepted = Unsigned16.box(ConfirmedRequestPDU.MaxAPDU_1476);
                        res.vendor_id                = Unsigned16.box(getVendorId());

                        sendApplicationRequest(res, sc.source, NetworkPriority.Normal);
                    });
                }
            }

            m_started = true;
        }

        startTransports();
    }

    public void close()
    {
        m_shutdown = true;

        if (m_listenerWhois != null)
        {
            m_listenerWhois.close();
            m_listenerWhois = null;
        }

        closeTransports();
    }

    private synchronized void closeTransports()
    {
        m_transports.forEach(AbstractTransport::close);
        m_transports.clear();
    }

    private synchronized void startTransports()
    {
        m_transports.forEach((transport) -> transport.start(this));
    }

    public boolean isClosed()
    {
        return m_shutdown;
    }

    //--//

    public EstimatedPayloadSize estimateSize(BACnetObjectIdentifier objId,
                                             BACnetPropertyIdentifierOrUnknown propId)
    {
        boolean              addInitialEstimate = false;
        EstimatedPayloadSize res;

        synchronized (m_payloadEstimations)
        {
            Map<BACnetPropertyIdentifierOrUnknown, EstimatedPayloadSize> map = m_payloadEstimations.get(objId.object_type);
            if (map == null)
            {
                m_payloadEstimations.put(objId.object_type, map = Maps.newHashMap());
            }

            res = map.get(propId);
            if (res == null)
            {
                map.put(propId, res = new EstimatedPayloadSize());
                addInitialEstimate = true;
            }
        }

        if (addInitialEstimate)
        {
            res.updateRequest(objId, propId);
        }

        return res;
    }

    //--//

    public boolean canReachAddress(InetAddress address)
    {
        for (AbstractTransport transport : m_transports)
        {
            if (transport.canReachAddress(address))
            {
                return true;
            }
        }

        return false;
    }

    //--//

    public Set<BACnetPropertyIdentifierOrUnknown> memoizeProperties(Set<BACnetPropertyIdentifierOrUnknown> set)
    {
        synchronized (m_memoizedProperties)
        {
            int size = set.size();

            for (Set<BACnetPropertyIdentifierOrUnknown> existingSet : m_memoizedProperties.get(size))
            {
                if (existingSet.equals(set))
                {
                    return existingSet;
                }
            }

            set = Collections.unmodifiableSet(Sets.newHashSet(set));
            m_memoizedProperties.put(size, set);
            return set;
        }
    }

    public BACnetObjectIdentifier memoize(BACnetObjectIdentifier id)
    {
        synchronized (m_memoizedObjectIds)
        {
            BACnetObjectIdentifier id2 = m_memoizedObjectIds.get(id);
            if (id2 != null)
            {
                return id2;
            }

            m_memoizedObjectIds.put(id, id);
            return id;
        }
    }

    //--//

    public <T extends ConfirmedServiceRequest, U extends ConfirmedServiceResponse<T>> ServiceRequestHandle<T, U> postNew(@NotNull DeviceIdentity target,
                                                                                                                         @NotNull TimeoutSpec timeoutSpec,
                                                                                                                         Duration minTimeout,
                                                                                                                         Duration maxTimeout,
                                                                                                                         Duration maxTimeoutPerOutstandingRequest,
                                                                                                                         @NotNull T req,
                                                                                                                         @NotNull Class<U> clz)
    {
        if (timeoutSpec == null)
        {
            LoggerInstance.error("Found call with no timeout spec: %s", new Exception());
            timeoutSpec = m_defaultTimeoutSpec;
        }

        return new ServiceRequestHandle<T, U>(this, target, timeoutSpec, minTimeout, maxTimeout, maxTimeoutPerOutstandingRequest, req, clz);
    }

    //--//

    public List<DeviceIdentity> getDevices(boolean onlyResolved)
    {
        List<DeviceIdentity> devices = Lists.newArrayList();

        synchronized (m_instanceToDevices)
        {
            for (LinkedList<DeviceIdentity> diList : m_instanceToDevices.values())
            {
                for (DeviceIdentity di : diList)
                {
                    if (onlyResolved && !di.isResolved())
                    {
                        continue;
                    }

                    devices.add(di);
                }
            }
        }

        devices.sort(DeviceIdentity.ComparatorInstance);

        return devices;
    }

    public DeviceIdentity locateDevice(BACnetDeviceAddress address,
                                       TransportAddress transport,
                                       boolean createIfMissing)
    {
        if (!address.hasValidInstanceNumber())
        {
            return null;
        }

        DeviceIdentity diNew;

        synchronized (m_instanceToDevices)
        {
            List<DeviceIdentity> diList = accessMultimap(m_instanceToDevices, address.instanceNumber, false);
            for (DeviceIdentity di : diList)
            {
                if (di.getNetworkAddress().networkNumber == address.networkNumber)
                {
                    if (!di.matchTransport(transport))
                    {
                        continue;
                    }

                    return di;
                }
            }

            if (!createIfMissing)
            {
                return null;
            }

            diNew = new DeviceIdentity(this, address);
            diList.add(diNew);
        }

        //
        // This part should happen outside the lock, because it can trigger future completions.
        //
        if (transport != null)
        {
            diNew.setTransportAddress(transport);
        }

        return diNew;
    }

    private DeviceIdentity processIAmRequest(ServiceContext sc,
                                             IAm req)
    {
        int networkNumber  = Unsigned16.unboxUnsignedOrDefault(sc.npdu.snet, BACnetAddress.LocalNetwork);
        int instanceNumber = req.i_am_device_identifier.instance_number.unbox();

        BACnetSegmentation  segmentation  = req.segmentation_supported;
        int                 maxApdu       = req.max_apdu_length_accepted.unboxUnsigned();
        BACnetDeviceAddress deviceAddress = new BACnetDeviceAddress(networkNumber, instanceNumber);

        debugVerbose("processIAmRequest: %s (%s) : %d/%d (%s : %d bytes)", sc.getEffectiveAddress(), sc.npdu.getSourceAddress(), networkNumber, instanceNumber, segmentation, maxApdu);

        return registerDevice(sc.getEffectiveAddress(), sc.npdu.getSourceAddress(), deviceAddress, segmentation, maxApdu);
    }

    //--//

    public void enableBBMDs()
    {
        for (AbstractTransport transport : m_transports)
        {
            for (TransportAddress bbmd : m_BBMDs)
            {
                transport.registerBBMD(bbmd);
            }

            transport.enableBBMDs();
        }
    }

    public CompletableFuture<Void> disableBBMDs() throws
                                                  Exception
    {
        for (AbstractTransport transport : m_transports)
        {
            await(transport.disableBBMDs());
        }

        return wrapAsync(null);
    }

    //--//

    public CompletableFuture<DeviceIdentity> resolveDeviceInstance(BACnetDeviceAddress address,
                                                                   TransportAddress transport,
                                                                   int timeout,
                                                                   TimeUnit unit)
    {
        DeviceIdentity di = locateDevice(address, transport, true);
        if (di == null)
        {
            return AsyncRuntime.asNull();
        }

        return di.waitForResolution(timeout, unit);
    }

    public DeviceIdentity registerDevice(BACnetDeviceDescriptor desc)
    {
        return registerDevice(desc.transport, desc.bacnetAddress, desc.address, desc.segmentation, desc.maxAdpu);
    }

    public DeviceIdentity registerDevice(TransportAddress transport,
                                         BACnetAddress macAddress,
                                         BACnetDeviceAddress address,
                                         BACnetSegmentation segmentation,
                                         int maxApdu)
    {
        if (!address.hasValidInstanceNumber())
        {
            // Don't register device/0, it's surely wrong.
            return null;
        }

        if (segmentation == null)
        {
            segmentation = BACnetSegmentation.no_segmentation;
        }

        if (maxApdu <= 0)
        {
            maxApdu = 100;
        }

        List<DeviceIdentity> updateList = Lists.newArrayList();
        DeviceIdentity       diTarget   = null;

        synchronized (m_instanceToDevices)
        {
            synchronized (m_transportState)
            {
                boolean              found = false;
                List<DeviceIdentity> diListForNumber;

                if (address.isWildcard())
                {
                    // Wildcard addresses don't have a valid instance number, skip lookup.
                    diListForNumber = null;
                }
                else
                {
                    diListForNumber = accessMultimap(m_instanceToDevices, address.instanceNumber, false);

                    //
                    // Update the info on device identities that already have a number.
                    //
                    for (DeviceIdentity di : diListForNumber)
                    {
                        if (di.matchNetworkId(address.networkNumber, address.instanceNumber))
                        {
                            if (transport != null && !transport.equals(di.getTransportAddress()))
                            {
                                continue;
                            }

                            found = true;
                            di.setNetworkCapabilities(segmentation, maxApdu);
                            diTarget = di;

                            if (!di.hasAddress())
                            {
                                updateList.add(di);
                            }
                        }
                    }
                }

                if (!found)
                {
                    DeviceIdentity diNew = new DeviceIdentity(this, address);
                    diNew.setNetworkCapabilities(segmentation, maxApdu);

                    if (diListForNumber != null)
                    {
                        diListForNumber.add(diNew);
                    }

                    updateList.add(diNew);

                    diTarget = diNew;
                }

                // Add to the 'm_ipToDevice' lookup map.
                if (transport != null && !updateList.isEmpty())
                {
                    TransportState transportState = accessTransportState(transport);
                    transportState.devices.addAll(updateList);
                }
            }
        }

        //
        // This part should happen outside the lock, because it can trigger future completions.
        //
        for (DeviceIdentity di : updateList)
        {
            di.setSourceAddress(macAddress);

            if (transport != null)
            {
                di.setTransportAddress(transport);
            }
        }

        return diTarget;
    }

    public void unregisterDevice(DeviceIdentity di)
    {
        BACnetDeviceAddress deviceAddress = di.getNetworkAddress();

        synchronized (m_instanceToDevices)
        {
            synchronized (m_transportState)
            {
                TransportState transportState = accessTransportState(di.getTransportAddress());

                removeDevice(transportState.devices, di);
            }

            LinkedList<DeviceIdentity> devices = m_instanceToDevices.get(deviceAddress.instanceNumber);
            if (devices != null)
            {
                removeDevice(devices, di);
            }
        }
    }

    private void removeDevice(LinkedList<DeviceIdentity> lst,
                              DeviceIdentity target)
    {
        lst.removeIf(di -> di == target);
    }

    //--//

    public CompletableFuture<AsyncSemaphore.Holder> acquireAccessToNetwork(DeviceIdentity target)
    {
        AsyncSemaphore semaphore;

        synchronized (m_transportState)
        {
            TransportState transportState = accessTransportState(target.getTransportAddress());
            NetworkState   networkState   = transportState.accessNetworkState(target.getNetworkAddress());
            semaphore = networkState.concurrentAccesses;
        }

        return semaphore.acquire();
    }

    public void recordNetworkRequestOutcome(DeviceIdentity target,
                                            boolean success,
                                            boolean timeout)
    {
        synchronized (m_transportState)
        {
            TransportState transportState = accessTransportState(target.getTransportAddress());
            NetworkState   networkState   = transportState.accessNetworkState(target.getNetworkAddress());

            networkState.recordNetworkRequestOutcome(target, success, timeout);
        }
    }

    public CompletableFuture<AsyncSemaphore.Holder> acquireAccessToTransport(DeviceIdentity target)
    {
        AsyncSemaphore semaphore;

        synchronized (m_transportState)
        {
            TransportState transportState = accessTransportState(target.getTransportAddress());
            semaphore = transportState.concurrentAccesses;
        }

        return semaphore.acquire();
    }

    void updateNetworkStatistics(DeviceIdentity target,
                                 TransportPerformanceCounters perf)
    {
        m_globalStatistics.accumulate(perf);

        synchronized (m_transportState)
        {
            TransportState transportState = accessTransportState(target.getTransportAddress());
            transportState.statistics.accumulate(perf);

            NetworkState networkState = transportState.accessNetworkState(target.getNetworkAddress());
            networkState.statistics.accumulate(perf);
        }
    }

    @FunctionalInterface
    public interface NetworkStatisticsCallback
    {
        void accept(TransportAddress ta,
                    TransportNetworkDescriptor desc,
                    TransportPerformanceCounters stats) throws
                                                        Exception;
    }

    public void enumerateNetworkStatistics(NetworkStatisticsCallback callback) throws
                                                                               Exception
    {
        TransportNetworkDescriptor d1 = new TransportNetworkDescriptor();
        callback.accept(null, d1, m_globalStatistics);

        //--//

        Map<TransportAddress, TransportState> transports;

        synchronized (m_transportState)
        {
            transports = Maps.newHashMap(m_transportState);
        }

        for (TransportAddress ta : transports.keySet())
        {
            TransportState ts = transports.get(ta);

            TransportNetworkDescriptor d2 = new TransportNetworkDescriptor();
            d2.transportAddress = ta.toString();
            d2.networkNumber    = -1;
            callback.accept(ta, d2, ts.statistics);

            //--//

            Map<Integer, NetworkState> networks;

            synchronized (m_transportState)
            {
                networks = Maps.newHashMap(ts.networks);
            }

            for (Integer networkNumber : networks.keySet())
            {
                NetworkState ns = networks.get(networkNumber);

                TransportNetworkDescriptor d3 = new TransportNetworkDescriptor();
                d3.transportAddress = d2.transportAddress;
                d3.networkNumber    = networkNumber;
                callback.accept(ta, d3, ns.statistics);
            }

            //--//

            List<DeviceIdentity> devices;

            synchronized (m_transportState)
            {
                devices = Lists.newArrayList(ts.devices);
            }

            for (DeviceIdentity device : devices)
            {
                BACnetDeviceAddress address = device.getNetworkAddress();

                TransportNetworkDescriptor d4 = new TransportNetworkDescriptor();
                d4.transportAddress = d2.transportAddress;
                d4.networkNumber    = address.networkNumber;
                d4.deviceIdentifier = Integer.toString(address.instanceNumber);
                callback.accept(ta, d4, device.getStatistics());
            }
        }
    }

    CompletableFuture<InvokeIdHolder> acquireInvokeId(DeviceIdentity target) throws
                                                                             Exception
    {
        while (true)
        {
            synchronized (m_transportState)
            {
                TransportState transportState = accessTransportState(target.getTransportAddress());
                InvokeIdHolder holder         = transportState.tryToAcquireInvokeId();
                if (holder != null)
                {
                    return wrapAsync(holder);
                }
            }

            // Sleep a little after a failure to acquire invokeId.
            await(sleep(100, TimeUnit.MILLISECONDS));
        }
    }

    InvokeIdListener registerForInvokeId(DeviceIdentity target,
                                         byte invokeId,
                                         IApplicationPduListener listener)
    {
        InvokeIdListener handle = new InvokeIdListener(this, target, invokeId, listener);

        synchronized (m_invokeIdToListeners)
        {
            WeakLinkedList<InvokeIdListener> lst = accessInvokeIdListeners(handle.getInvokeId());
            lst.add(handle);
        }

        return handle;
    }

    void removeListener(InvokeIdListener handle)
    {
        synchronized (m_invokeIdToListeners)
        {
            WeakLinkedList<InvokeIdListener> lst = accessInvokeIdListeners(handle.getInvokeId());
            lst.remove(handle);
        }
    }

    List<IApplicationPduListener> locateListeners(TransportAddress source,
                                                  BACnetAddress sourceSpecified,
                                                  byte invokeId)
    {
        List<IApplicationPduListener> res = Lists.newArrayList();

        synchronized (m_invokeIdToListeners)
        {
            for (InvokeIdListener v : accessInvokeIdListeners(invokeId))
            {
                if (v.isMatch(source, sourceSpecified))
                {
                    res.add(v.getListener());
                }
            }
        }

        return res;
    }

    //--//

    public <T extends BaseVirtualLinkLayer> LinkLayerListenerHandle<T> registerLinkLayerListener(Class<T> clz,
                                                                                                 BiConsumer<T, ServiceContext> callback)
    {
        LinkLayerListenerHandle<T> t = new LinkLayerListenerHandle<>(this, clz, callback);

        synchronized (m_listenersForLinkLayer)
        {
            List<LinkLayerListenerHandle<? extends BaseVirtualLinkLayer>> lst = accessMultimap(m_listenersForLinkLayer, clz, false);
            lst.add(t);
        }

        return t;
    }

    void removeListener(Class<? extends BaseVirtualLinkLayer> clz,
                        LinkLayerListenerHandle<? extends BaseVirtualLinkLayer> listenerHandle)
    {
        synchronized (m_listenersForLinkLayer)
        {
            List<LinkLayerListenerHandle<? extends BaseVirtualLinkLayer>> lst = accessMultimap(m_listenersForLinkLayer, clz, false);
            lst.remove(listenerHandle);
        }
    }

    void processRequest(BaseVirtualLinkLayer req,
                        ServiceContext sc)
    {
        boolean                               processed = false;
        Class<? extends BaseVirtualLinkLayer> clz       = req.getClass();
        while (clz != null)
        {
            List<LinkLayerListenerHandle<? extends BaseVirtualLinkLayer>> lst;

            synchronized (m_listenersForLinkLayer)
            {
                // Copy the list under lock.
                lst = accessMultimap(m_listenersForLinkLayer, clz, true);
            }

            // Enumerate the list outside the lock.
            for (LinkLayerListenerHandle<? extends BaseVirtualLinkLayer> listener : lst)
            {
                listener.invoke(req, sc);
                processed = true;
            }

            if (clz == BaseVirtualLinkLayer.class)
            {
                break;
            }

            @SuppressWarnings("unchecked") Class<? extends BaseVirtualLinkLayer> clz2 = (Class<? extends BaseVirtualLinkLayer>) clz.getSuperclass();
            clz = clz2;
        }

        if (isEnabled(Severity.DebugObnoxious))
        {
            debugObnoxious("processRequest: %s\nGot request of type %s\n%s", sc.getEffectiveAddress(), req.getClass(), ObjectMappers.prettyPrintAsJson(req));
        }

        if (!processed)
        {
            debug("processRequest: No taker for request of type %s", req.getClass());
        }
    }

    //--//

    public <T extends NetworkMessagePDU> NetworkMessageListenerHandle<T> registerNetworkMessageListener(Class<T> clz,
                                                                                                        BiConsumer<T, ServiceContext> callback)
    {
        NetworkMessageListenerHandle<T> t = new NetworkMessageListenerHandle<>(this, clz, callback);

        synchronized (m_listenersForNetworkMessage)
        {
            List<NetworkMessageListenerHandle<? extends NetworkMessagePDU>> lst = accessMultimap(m_listenersForNetworkMessage, clz, false);

            lst.add(t);
        }

        return t;
    }

    void removeListener(Class<? extends NetworkMessagePDU> clz,
                        NetworkMessageListenerHandle<? extends NetworkMessagePDU> listenerHandle)
    {
        synchronized (m_listenersForNetworkMessage)
        {
            List<NetworkMessageListenerHandle<? extends NetworkMessagePDU>> lst = accessMultimap(m_listenersForNetworkMessage, clz, false);

            lst.remove(listenerHandle);
        }
    }

    void processRequest(NetworkMessagePDU req,
                        ServiceContext sc)
    {
        boolean                            processed = false;
        Class<? extends NetworkMessagePDU> clz       = req.getClass();
        while (clz != null)
        {
            List<NetworkMessageListenerHandle<? extends NetworkMessagePDU>> lst;

            synchronized (m_listenersForNetworkMessage)
            {
                // Copy the list under lock.
                lst = accessMultimap(m_listenersForNetworkMessage, clz, true);
            }

            // Enumerate the list outside the lock.
            for (NetworkMessageListenerHandle<? extends NetworkMessagePDU> listener : lst)
            {
                listener.invoke(req, sc);
                processed = true;
            }

            if (clz == NetworkMessagePDU.class)
            {
                break;
            }

            @SuppressWarnings("unchecked") Class<? extends NetworkMessagePDU> clz2 = (Class<? extends NetworkMessagePDU>) clz.getSuperclass();
            clz = clz2;
        }

        if (isEnabled(Severity.DebugObnoxious))
        {
            debugObnoxious("processRequest: %s (%s)\nGot request of type %s\n%s", sc.getEffectiveAddress(), sc.npdu.getSourceAddress(), req.getClass(), ObjectMappers.prettyPrintAsJson(req));
        }

        if (!processed)
        {
            debug("processRequest: No taker for request of type %s", req.getClass());
        }
    }

    //--//

    public <T extends UnconfirmedServiceRequest> ServiceListenerHandle<T> registerListener(Class<T> clz,
                                                                                           BiConsumer<T, ServiceContext> callback)
    {
        ServiceListenerHandle<T> t = new ServiceListenerHandle<>(this, clz, callback);

        synchronized (m_listeners)
        {
            List<ServiceListenerHandle<? extends ServiceCommon>> lst = accessMultimap(m_listeners, clz, false);

            lst.add(t);
        }

        return t;
    }

    void removeListener(Class<? extends ServiceCommon> clz,
                        ServiceListenerHandle<? extends ServiceCommon> listenerHandle)
    {
        synchronized (m_listeners)
        {
            List<ServiceListenerHandle<? extends ServiceCommon>> lst = accessMultimap(m_listeners, clz, false);

            lst.remove(listenerHandle);
        }
    }

    void processRequest(UnconfirmedServiceRequest req,
                        ServiceContext sc)
    {
        boolean                                    processed = false;
        Class<? extends UnconfirmedServiceRequest> clz       = req.getClass();
        while (clz != null)
        {
            List<ServiceListenerHandle<? extends ServiceCommon>> lst;

            synchronized (m_listeners)
            {
                // Copy the list under lock.
                lst = accessMultimap(m_listeners, clz, true);
            }

            // Enumerate the list outside the lock.
            for (ServiceListenerHandle<? extends ServiceCommon> listener : lst)
            {
                listener.invoke(req, sc);
                processed = true;
            }

            if (clz == UnconfirmedServiceRequest.class)
            {
                break;
            }

            @SuppressWarnings("unchecked") Class<? extends UnconfirmedServiceRequest> clz2 = (Class<? extends UnconfirmedServiceRequest>) clz.getSuperclass();
            clz = clz2;
        }

        if (isEnabled(Severity.DebugObnoxious))
        {
            debugObnoxious("processRequest: %s (%s)\nGot request of type %s\n%s", sc.getEffectiveAddress(), sc.npdu.getSourceAddress(), req.getClass(), ObjectMappers.prettyPrintAsJson(req));
        }

        if (!processed)
        {
            IAm iam = Reflection.as(req, IAm.class);
            if (iam != null)
            {
                processIAmRequest(sc, iam);
            }
            else
            {
                debug("processRequest: No taker for request of type %s", req.getClass());
            }
        }
    }

    //--//

    public ServiceContext allocateServiceContext(TransportAddress source,
                                                 int packetLength)
    {
        return new ServiceContext(this, source, packetLength);
    }

    //--//

    public void sendNetworkRequest(NetworkMessagePDU request,
                                   TransportAddress destination,
                                   NetworkPriority priority)
    {
        Preconditions.checkNotNull(destination);

        sendRequest(null, request, destination, priority);
    }

    public void sendNetworkBroadcastRequest(NetworkMessagePDU request,
                                            NetworkPriority priority)
    {
        sendRequest(null, request, null, priority);
    }

    //--//

    public void sendApplicationRequest(UnconfirmedServiceRequest request,
                                       TransportAddress destination,
                                       NetworkPriority priority)
    {
        Preconditions.checkNotNull(destination);

        sendRequest(request, null, destination, priority);
    }

    public void sendApplicationBroadcastRequest(UnconfirmedServiceRequest request,
                                                NetworkPriority priority)
    {
        sendRequest(request, null, null, priority);
    }

    private void sendRequest(UnconfirmedServiceRequest applicationRequest,
                             NetworkMessagePDU networkRequest,
                             TransportAddress destination,
                             NetworkPriority priority)
    {
        for (AbstractTransport transport : m_transports)
        {
            sendRequest(transport, applicationRequest, networkRequest, destination, priority);
        }
    }

    private void sendRequest(AbstractTransport transport,
                             UnconfirmedServiceRequest applicationRequest,
                             NetworkMessagePDU networkRequest,
                             TransportAddress destination,
                             NetworkPriority priority)
    {
        try (OutputBuffer ob = new OutputBuffer())
        {
            boolean isBroadcast = (destination == null);

            //
            // Prepare NPDU
            //
            NetworkPDU npdu = new NetworkPDU();
            npdu.hop_count = Unsigned8.box(255);
            npdu.priority  = priority;

            if (networkRequest != null)
            {
                npdu.network_message = true;
                npdu.message_type    = MessageType.parse(networkRequest.getClass());
            }

            if (isBroadcast)
            {
                npdu.setDestinationAddress(BACnetAddress.GlobalBroadcast);
            }
            else if (!transport.canSend(destination))
            {
                return;
            }

            transport.setSourceAddress(npdu, getNetworkNumber());

            npdu.encode(ob);

            if (networkRequest != null)
            {

                //
                // Add NPDU.
                //
                networkRequest.encodeHeader(ob);
            }
            else
            {
                //
                // Add APDU.
                //
                UnconfirmedRequestPDU pdu = applicationRequest.preparePCI();
                pdu.encodeHeader(ob);

                try (OutputBuffer obPayload = applicationRequest.encodePayload())
                {
                    ob.emitNestedBlock(obPayload);
                }
            }

            if (isBroadcast)
            {
                transport.sendBroadcast(ob);
            }
            else
            {
                transport.sendDirect(ob, destination);
            }
        }
    }

    int sendDirect(TransportAddress destination,
                   OutputBuffer ob)
    {
        for (AbstractTransport transport : m_transports)
        {
            if (transport.canSend(destination))
            {
                return transport.sendDirect(ob, destination);
            }
        }

        throw Exceptions.newRuntimeException("No route for %s", destination);
    }

    private TransportState accessTransportState(TransportAddress key)
    {
        TransportState state = m_transportState.get(key);
        if (state == null)
        {
            state = new TransportState(key);
            m_transportState.put(key, state);
        }

        return state;
    }

    private WeakLinkedList<InvokeIdListener> accessInvokeIdListeners(byte invokeId)
    {
        int index = invokeId & 0xFF;

        WeakLinkedList<InvokeIdListener> lst = m_invokeIdToListeners[index];
        if (lst == null)
        {
            lst                          = new WeakLinkedList<>();
            m_invokeIdToListeners[index] = lst;
        }

        return lst;
    }

    private <K, V> List<V> accessMultimap(Map<K, LinkedList<V>> map,
                                          K key,
                                          boolean makeCopy)
    {
        LinkedList<V> values = map.get(key);
        if (values == null)
        {
            if (makeCopy)
            {
                return Collections.emptyList();
            }

            values = Lists.newLinkedList();
            map.put(key, values);
        }

        if (makeCopy)
        {
            return Lists.newArrayList(values);
        }
        else
        {
            return values;
        }
    }

    //--//

    private static class State
    {
        private final Set<DeviceIdentity> discoveredDevices = Sets.newHashSet();

        private final AtomicInteger passesWithNoProgress = new AtomicInteger();
        private final AtomicInteger pendingNetworks      = new AtomicInteger();

        private int[] sortDiscoveredDevices()
        {
            Set<Integer> instanceNumbersAsSet = Sets.newHashSet();

            synchronized (this)
            {
                for (DeviceIdentity di : discoveredDevices)
                {
                    instanceNumbersAsSet.add(di.getDeviceId().instance_number.unbox());
                }
            }

            // Add sentinels, to make it easier to iterate over ranges.
            instanceNumbersAsSet.add(BACnetObjectIdentifier.MIN_INSTANCE_NUMBER - 1);
            instanceNumbersAsSet.add(BACnetObjectIdentifier.MAX_INSTANCE_NUMBER + 1);

            List<Integer> instanceNumbersAsList = Lists.newArrayList(instanceNumbersAsSet);

            instanceNumbersAsList.sort(Integer::compareTo);

            int[] instanceNumbers = new int[instanceNumbersAsList.size()];
            int   pos             = 0;

            for (int instanceNumber : instanceNumbersAsList)
            {
                instanceNumbers[pos++] = instanceNumber;
            }

            return instanceNumbers;
        }

        private boolean add(DeviceIdentity di)
        {
            synchronized (this)
            {
                return di != null && discoveredDevices.add(di);
            }
        }
    }

    public CompletableFuture<Set<DeviceIdentity>> scanForDevices(ILogger logger,
                                                                 int maxRetries,
                                                                 WhoIsRange limitScan,
                                                                 boolean sweepMstp,
                                                                 boolean includeNetworksFromRouters,
                                                                 Multimap<TransportAddress, Integer> nonDiscoverableMstpTrunks,
                                                                 BiConsumer<IAm, DeviceIdentity> progressCallback) throws
                                                                                                                   Exception
    {
        State state = new State();

        enableBBMDs();

        // Wait a bit to let the registration go through.
        await(sleep(1, TimeUnit.SECONDS));

        for (AbstractTransport transport : m_transports)
        {
            try (ServiceListenerHandle<IAm> listener = registerListener(IAm.class, (msg, sc) ->
            {
                //
                // We want to scan one transport at a time.
                //
                if (!transport.canSend(sc.getEffectiveAddress()))
                {
                    //
                    // Sometimes we receive a reply on a different transport.
                    // Skip it.
                    //
                    return;
                }

                DeviceIdentity di = processIAmRequest(sc, msg);
                if (di == null)
                {
                    return;
                }

                if (state.add(di))
                {
                    state.passesWithNoProgress.set(0);

                    if (progressCallback != null)
                    {
                        progressCallback.accept(msg, di);
                    }
                }
            }))
            {
                Map<WhoIsRange, WhoIsRange> rangeStatistics = Maps.newHashMap();

                List<WhoIsRange> ranges = Lists.newArrayList();
                Random           rnd    = new Random();

                state.passesWithNoProgress.set(0);

                while (state.passesWithNoProgress.get() < maxRetries)
                {
                    int[] instanceNumbers = state.sortDiscoveredDevices();
                    logger.debug("Sending WhoIs for %d distinct ranges", instanceNumbers.length - 1);

                    for (int pos = 1; pos < instanceNumbers.length; pos++)
                    {
                        int low  = instanceNumbers[pos - 1] + 1;
                        int high = instanceNumbers[pos] - 1;

                        if (limitScan != null)
                        {
                            low  = Math.max(low, limitScan.low);
                            high = Math.min(high, limitScan.high);
                        }

                        if (low <= high)
                        {
                            WhoIsRange range = new WhoIsRange(low, high);

                            WhoIsRange oldRange = rangeStatistics.get(range);
                            if (oldRange == null)
                            {
                                rangeStatistics.put(range, range);
                                oldRange = range;
                            }

                            ranges.add(oldRange);
                        }
                    }

                    if (ranges.isEmpty())
                    {
                        break;
                    }

                    logger.info("ScanForDevices: %d ranges...", ranges.size());

                    state.passesWithNoProgress.incrementAndGet();

                    for (int size = ranges.size(); size > 0; size--)
                    {
                        // To avoid sending out requests in the same order, we randomize the list.
                        int        index = rnd.nextInt(size);
                        WhoIsRange range = ranges.remove(index);

                        logger.debugVerbose("Sending WhoIs for range [%d-%d]", range.low, range.high);
                        WhoIs whoIs = new WhoIs();
                        whoIs.setRange(range.low, range.high);
                        sendRequest(transport, whoIs, null, null, NetworkPriority.Normal);

                        // Limit rate.
                        await(sleep(50, TimeUnit.MILLISECONDS));
                    }

                    await(sleep(500, TimeUnit.MILLISECONDS));

                    logger.info("ScanForDevices: found %d devices...", state.discoveredDevices.size());
                }
            }
        }

        if (sweepMstp || nonDiscoverableMstpTrunks != null)
        {
            TimeoutSpec timeSpec = TimeoutSpec.create(maxRetries, Duration.of(250, ChronoUnit.MILLIS));
            MstpSweep   sweep    = new MstpSweep(state, sweepMstp, includeNetworksFromRouters, nonDiscoverableMstpTrunks);

            CompletableFuture<Void> worker = sweep.execute(logger, timeSpec);

            while (!worker.isDone())
            {
                await(sleep(30, TimeUnit.SECONDS));

                logger.info("ScanForDevices: (waiting for %d MS/TP sweeps) found %d devices in total...", state.pendingNetworks.get(), state.discoveredDevices.size());
            }

            await(worker);
        }

        await(disableBBMDs());

        return wrapAsync(state.discoveredDevices);
    }

    public CompletableFuture<Set<DeviceIdentity>> scanForDevicesInMstpTruck(ILogger logger,
                                                                            TransportAddress ta,
                                                                            int networkNumber,
                                                                            TimeoutSpec timeSpec) throws
                                                                                                  Exception
    {
        State state = new State();

        MstpNetworkSweep sweep = new MstpNetworkSweep(state, networkNumber);

        await(sweep.scan(logger, ta, timeSpec));

        return wrapAsync(state.discoveredDevices);
    }

    //--//

    class MstpNetworkSweep
    {
        private final State         m_state;
        private final int           m_networkNumber;
        private final AtomicInteger m_newDevices = new AtomicInteger();

        private Integer   m_commonInstanceOffset;
        private boolean[] m_instances = new boolean[256];

        private MstpNetworkSweep(State state,
                                 int networkNumber)
        {
            m_state         = state;
            m_networkNumber = networkNumber;
        }

        void setCommonInstanceOffset(int commonInstanceOffset)
        {
            m_commonInstanceOffset = commonInstanceOffset;
        }

        void sniff(DeviceIdentity di)
        {
            BACnetAddress addr = di.getBACnetAddress();
            if (addr != null && addr.couldBeMstp())
            {
                int instance = addr.mac_address[0];

                m_instances[instance] = true;

                int baseInstance = di.getInstanceNumber() - instance;

                if (m_commonInstanceOffset == null)
                {
                    m_commonInstanceOffset = baseInstance;
                }
                else if (m_commonInstanceOffset != baseInstance)
                {
                    m_commonInstanceOffset = -1;
                }
            }
        }

        private CompletableFuture<Void> scan(ILogger logger,
                                             TransportAddress ta,
                                             TimeoutSpec timeSpec) throws
                                                                   Exception
        {
            logger.info("ScanForDevices: Queueing MS/TP network %d for scanning at %s...", m_networkNumber, ta);

            m_state.pendingNetworks.incrementAndGet();

            try
            {
                AsyncWaitMultiple waiter = new AsyncWaitMultiple();

                for (int i = 1; i < 255; i++)
                {
                    if (!m_instances[i])
                    {
                        waiter.add(scanSingle(logger, timeSpec, ta, i));

                        await(sleep(10, TimeUnit.MILLISECONDS));

                        if (waiter.getPendingCount() >= 8)
                        {
                            await(waiter.drain());
                        }
                    }
                }

                await(waiter.drain());
            }
            catch (Throwable t)
            {
                logger.error("ScanForDevices: Scan of MS/TP network %d at %s failed, due to %s", m_networkNumber, ta, t);
            }

            m_state.pendingNetworks.decrementAndGet();

            logger.info("ScanForDevices: Completed scan of MS/TP network %d at %s: found %d extra devices", m_networkNumber, ta, m_newDevices.get());

            return wrapAsync(null);
        }

        private CompletableFuture<Void> scanSingle(ILogger logger,
                                                   TimeoutSpec timeSpec,
                                                   TransportAddress ta,
                                                   int instanceNumber) throws
                                                                       Exception
        {
            BACnetAddress bacnetAddress = BACnetAddress.createMstp(m_networkNumber, instanceNumber);

            logger.debug("ScanForDevices: checking %s %s", ta, bacnetAddress);

            DeviceIdentity di = await(probeTarget(logger, ta, bacnetAddress, m_networkNumber, timeSpec));
            if (di == null)
            {
                if (m_commonInstanceOffset != null && m_commonInstanceOffset >= 0)
                {
                    BACnetDeviceAddress deviceAddress = new BACnetDeviceAddress(m_networkNumber, m_commonInstanceOffset + instanceNumber);
                    di = await(probeTarget(logger, ta, bacnetAddress, deviceAddress, timeSpec));
                }
            }

            logger.debug("ScanForDevices: checked %s %s", ta, bacnetAddress);

            if (di != null && m_state.add(di))
            {
                logger.debug("ScanForDevices: found %s...", di);
                m_newDevices.incrementAndGet();
            }

            return wrapAsync(null);
        }
    }

    private class MstpSweep
    {
        private final State                               m_state;
        private final boolean                             m_sweepMstp;
        private final boolean                             m_includeNetworksFromRouters;
        private final Multimap<TransportAddress, Integer> m_nonDiscoverableMstpTrunks;

        MstpSweep(State state,
                  boolean sweepMstp,
                  boolean includeNetworksFromRouters,
                  Multimap<TransportAddress, Integer> nonDiscoverableMstpTrunks)
        {
            m_state                      = state;
            m_sweepMstp                  = sweepMstp;
            m_includeNetworksFromRouters = includeNetworksFromRouters;
            m_nonDiscoverableMstpTrunks  = nonDiscoverableMstpTrunks;
        }

        private final Map<TransportAddress, List<MstpNetworkSweep>> m_networks = Maps.newHashMap();

        CompletableFuture<Void> execute(ILogger logger,
                                        TimeoutSpec timeSpec) throws
                                                              Exception
        {
            if (m_includeNetworksFromRouters)
            {
                //
                // Collect network numbers from all the routers.
                //
                await(scanForRouters(-1, (msg, ta) ->
                {
                    if (msg.networks != null)
                    {
                        for (Unsigned16 netId : msg.networks)
                        {
                            ensure(ta, netId.unboxUnsigned());
                        }
                    }
                }, 5, TimeUnit.SECONDS));
            }

            if (m_sweepMstp)
            {
                //
                // Mark devices we already found.
                //
                for (DeviceIdentity di : m_state.discoveredDevices)
                {
                    int networkNumber = di.getNetworkNumber();
                    if (networkNumber > 0)
                    {
                        MstpNetworkSweep stateSub = ensure(di.getTransportAddress(), networkNumber);

                        stateSub.sniff(di);
                    }
                }
            }

            if (m_nonDiscoverableMstpTrunks != null)
            {
                for (TransportAddress ta : m_nonDiscoverableMstpTrunks.keys())
                {
                    for (Integer networkNumber : m_nonDiscoverableMstpTrunks.get(ta))
                    {
                        ensure(ta, networkNumber);
                    }
                }
            }

            //
            // Sweep each network.
            //
            AsyncWaitMultiple waiter = new AsyncWaitMultiple();

            for (TransportAddress ta : m_networks.keySet())
            {
                List<MstpNetworkSweep> networks = m_networks.get(ta);

                for (MstpNetworkSweep network : networks)
                {
                    waiter.add(network.scan(logger, ta, timeSpec));
                }
            }

            await(waiter.drain());

            return wrapAsync(null);
        }

        private MstpNetworkSweep ensure(TransportAddress ta,
                                        int networkNumber)
        {
            List<MstpNetworkSweep> lst = m_networks.computeIfAbsent(ta, k -> Lists.newArrayList());
            for (MstpNetworkSweep state : lst)
            {
                if (state.m_networkNumber == networkNumber)
                {
                    return state;
                }
            }

            MstpNetworkSweep state = new MstpNetworkSweep(m_state, networkNumber);
            lst.add(state);
            return state;
        }
    }

    //--//

    public CompletableFuture<Void> scanForRouters(int networkNumber,
                                                  BiConsumer<IAmRouterToNetworkPDU, TransportAddress> progressCallback,
                                                  int timeout,
                                                  TimeUnit unit) throws
                                                                 Exception
    {
        try (NetworkMessageListenerHandle<IAmRouterToNetworkPDU> listener = registerNetworkMessageListener(IAmRouterToNetworkPDU.class, (msg, sc) ->
        {
            if (progressCallback != null)
            {
                progressCallback.accept(msg, sc.getEffectiveAddress());
            }
        }))
        {
            WhoIsRouterToNetworkPDU whoIs = new WhoIsRouterToNetworkPDU();
            if (networkNumber >= 0)
            {
                whoIs.networkNumber = Optional.of(Unsigned16.box(networkNumber));
            }
            sendNetworkBroadcastRequest(whoIs, NetworkPriority.Normal);

            await(sleep(timeout, unit));
        }

        return wrapAsync(null);
    }

    //--//

    public CompletableFuture<DeviceIdentity> probeTarget(ILogger logger,
                                                         TransportAddress ta,
                                                         BACnetAddress macAddress,
                                                         int networkNumber,
                                                         TimeoutSpec timeSpec) throws
                                                                               Exception
    {
        return probeTarget(logger, ta, macAddress, networkNumber, BACnetObjectIdentifier.MAX_INSTANCE_NUMBER, timeSpec);
    }

    public CompletableFuture<DeviceIdentity> probeTarget(ILogger logger,
                                                         TransportAddress ta,
                                                         BACnetAddress macAddress,
                                                         int networkNumber,
                                                         int instanceNumber,
                                                         TimeoutSpec timeSpec) throws
                                                                               Exception
    {
        BACnetDeviceAddress deviceAddress = new BACnetDeviceAddress(networkNumber, instanceNumber);

        return probeTarget(logger, ta, macAddress, deviceAddress, timeSpec);
    }

    public CompletableFuture<DeviceIdentity> probeTarget(ILogger logger,
                                                         final TransportAddress ta,
                                                         final BACnetAddress macAddress,
                                                         final BACnetDeviceAddress deviceAddress,
                                                         final TimeoutSpec timeSpec) throws
                                                                                     Exception
    {
        if (logger == null)
        {
            logger = LoggerInstance;
        }

        AsyncMutex mutex;

        if (deviceAddress.isWildcard())
        {
            synchronized (m_pendingProbesWildcard)
            {
                mutex = m_pendingProbesWildcard.computeIfAbsent(ta, (key) -> new AsyncMutex());
            }
        }
        else
        {
            synchronized (m_pendingProbes)
            {
                mutex = m_pendingProbes.computeIfAbsent(deviceAddress, (key) -> new AsyncMutex());
            }
        }

        try (AsyncMutex.Holder holder = await(mutex.acquire()))
        {
            //
            // To check if there's a device at a certain address, we create a new DeviceIdentity and try to read a couple of properties.
            //
            DeviceIdentity di = registerDevice(ta, macAddress, deviceAddress, BACnetSegmentation.no_segmentation, 100);
            if (di != null)
            {
                try
                {
                    logger.debugVerbose("probeTarget: %s", di);

                    BACnetDeviceAddress actualAddress = null;
                    device              dev           = new device();

                    {
                        ServiceRequestResult<ReadProperty.Ack> req1 = await(di.readPropertyRaw(timeSpec, di.getDeviceId(), BACnetPropertyIdentifier.object_identifier));
                        ReadProperty.Ack                       ack1 = req1.value;
                        if (ack1 != null)
                        {
                            logger.debugVerbose("probeTarget: %s: GOT REPLY => %s", di, ObjectMappers.prettyPrintAsJson(ack1));

                            ack1.updateObject(logger, dev);

                            actualAddress = createTargetAddress(deviceAddress.networkNumber, dev.object_identifier);

                            logger.debugVerbose("probeTarget: %s: UPDATE => %s / %s", di, actualAddress, ObjectMappers.prettyPrintAsJson(dev));
                        }
                    }

                    {
                        ServiceRequestResult<ReadProperty.Ack> req1 = await(di.readPropertyRaw(timeSpec, di.getDeviceId(), BACnetPropertyIdentifier.max_apdu_length_accepted));
                        ReadProperty.Ack                       ack1 = req1.value;
                        if (ack1 != null)
                        {
                            logger.debugVerbose("probeTarget: %s: GOT REPLY => %s", di, ObjectMappers.prettyPrintAsJson(ack1));

                            ack1.updateObject(logger, dev);

                            if (actualAddress == null || actualAddress.isWildcard())
                            {
                                actualAddress = createTargetAddress(deviceAddress.networkNumber, ack1.object_identifier);

                                logger.debugVerbose("probeTarget: %s: UPDATE => %s / %s", di, actualAddress, ObjectMappers.prettyPrintAsJson(dev));
                            }
                        }
                    }

                    if (actualAddress == null)
                    {
                        ServiceRequestResult<ReadPropertyMultiple.Ack> req2 = await(di.readPropertyMultipleRaw(timeSpec,
                                                                                                               null,
                                                                                                               null,
                                                                                                               null,
                                                                                                               di.getDeviceId(),
                                                                                                               BACnetPropertyIdentifier.object_name,
                                                                                                               BACnetPropertyIdentifier.max_apdu_length_accepted));

                        ReadPropertyMultiple.Ack ack2 = req2.value;
                        if (ack2 != null)
                        {
                            logger.debugVerbose("probeTarget: %s: GOT REPLY => %s", di, ObjectMappers.prettyPrintAsJson(ack2));

                            for (ReadAccessResult result : ack2.list_of_read_access_results)
                            {
                                if (actualAddress == null || actualAddress.isWildcard())
                                {
                                    actualAddress = createTargetAddress(deviceAddress.networkNumber, result.object_identifier);

                                    logger.debugVerbose("probeTarget: %s: UPDATE => %s / %s", di, actualAddress, ObjectMappers.prettyPrintAsJson(dev));
                                }

                                result.updateObject(logger, dev);
                            }
                        }
                    }

                    if (actualAddress != null)
                    {
                        ServiceRequestResult<ReadProperty.Ack> req3 = await(di.readPropertyRaw(timeSpec, di.getDeviceId(), BACnetPropertyIdentifier.object_identifier));
                        ReadProperty.Ack                       ack3 = req3.value;
                        if (ack3 != null)
                        {
                            logger.debugVerbose("probeTarget: %s: GOT REPLY => %s", di, ObjectMappers.prettyPrintAsJson(ack3));

                            ack3.updateObject(logger, dev);

                            BACnetDeviceAddress actualAddress2 = createTargetAddress(deviceAddress.networkNumber, dev.object_identifier);
                            if (actualAddress2 != null)
                            {
                                actualAddress = actualAddress2;
                            }
                        }

                        if (!actualAddress.isWildcard())
                        {
                            DeviceIdentity diResult = registerDevice(ta, macAddress, actualAddress, BACnetSegmentation.no_segmentation, (int) dev.max_apdu_length_accepted);
                            if (diResult != null)
                            {
                                if (diResult == di)
                                {
                                    di = null;
                                }

                                logger.debug("probeTarget: %s: SUCCESS => %s", di, diResult);

                                return wrapAsync(diResult);
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    logger.debugVerbose("probeTarget: %s: FAILURE => %s", di, e);
                }
                finally
                {
                    if (di != null)
                    {
                        unregisterDevice(di);
                    }
                }
            }
        }

        return wrapAsync(null);
    }

    private static BACnetDeviceAddress createTargetAddress(int networkNumber,
                                                           BACnetObjectIdentifier objId)
    {
        if (objId != null)
        {
            int instanceNumber = objId.instance_number.unbox();
            if (instanceNumber != BACnetObjectIdentifier.MAX_INSTANCE_NUMBER)
            {
                return new BACnetDeviceAddress(networkNumber, instanceNumber);
            }
        }

        return null;
    }

    //--//

    public CompletableFuture<Void> scanForBBMDs(NetworkHelper.InetAddressWithPrefix subnet,
                                                BiConsumer<InetSocketAddress, List<ForeignDeviceTableEntry>> progressCallback,
                                                int retries,
                                                int timeout,
                                                TimeUnit unit) throws
                                                               Exception
    {
        AsyncWaitMultiple waiter = new AsyncWaitMultiple();

        for (AbstractTransport transport : m_transports)
        {
            UdpTransport udp = Reflection.as(transport, UdpTransport.class);
            if (udp != null)
            {
                waiter.add(scanSubnetForBBMDs(udp, subnet, progressCallback, retries, timeout, unit));
            }
        }

        return waiter.drain();
    }

    private CompletableFuture<Void> scanSubnetForBBMDs(UdpTransport transport,
                                                       NetworkHelper.InetAddressWithPrefix subnet,
                                                       BiConsumer<InetSocketAddress, List<ForeignDeviceTableEntry>> progressCallback,
                                                       int retries,
                                                       int timeout,
                                                       TimeUnit unit) throws
                                                                      Exception
    {
        AsyncWaitMultiple waiter = new AsyncWaitMultiple();

        for (int offset = 1; offset < subnet.getSize(); offset++)
        {
            InetAddress address = subnet.generateAddress(offset);

            waiter.add(checkAddressForBBMD(transport, address, progressCallback, retries, timeout, unit));

            await(sleep(20, TimeUnit.MILLISECONDS));
        }

        await(waiter.drain());

        return wrapAsync(null);
    }

    private CompletableFuture<Void> checkAddressForBBMD(UdpTransport transport,
                                                        InetAddress address,
                                                        BiConsumer<InetSocketAddress, List<ForeignDeviceTableEntry>> progressCallback,
                                                        int retries,
                                                        int timeout,
                                                        TimeUnit unit)
    {
        try
        {
            InetSocketAddress             target = new InetSocketAddress(address, transport.getNetworkPort());
            List<ForeignDeviceTableEntry> table  = await(transport.readForeignDeviceTable(target, retries, timeout, unit));

            if (table != null)
            {
                progressCallback.accept(target, table);
            }
        }
        catch (Throwable t)
        {
            // Ignore failures.
        }

        return wrapAsync(null);
    }
}

