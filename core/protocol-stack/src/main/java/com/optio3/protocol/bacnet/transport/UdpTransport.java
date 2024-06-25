/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.transport;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.optio3.concurrency.Executors;
import com.optio3.infra.NetworkHelper;
import com.optio3.lang.Unsigned16;
import com.optio3.logging.Severity;
import com.optio3.protocol.bacnet.BACnetManager;
import com.optio3.protocol.bacnet.LinkLayerListenerHandle;
import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.protocol.bacnet.model.enums.NetworkPriority;
import com.optio3.protocol.bacnet.model.linklayer.BaseVirtualLinkLayer;
import com.optio3.protocol.bacnet.model.linklayer.BroadcastDistributionTableEntry;
import com.optio3.protocol.bacnet.model.linklayer.DistributeBroadcastToNetwork;
import com.optio3.protocol.bacnet.model.linklayer.ForeignDeviceTableEntry;
import com.optio3.protocol.bacnet.model.linklayer.OriginalBroadcast;
import com.optio3.protocol.bacnet.model.linklayer.OriginalUnicast;
import com.optio3.protocol.bacnet.model.linklayer.ReadBroadcastDistributionTable;
import com.optio3.protocol.bacnet.model.linklayer.ReadForeignDeviceTable;
import com.optio3.protocol.bacnet.model.linklayer.RegisterForeignDevice;
import com.optio3.protocol.bacnet.model.pdu.NetworkPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ConfirmedRequestPDU;
import com.optio3.protocol.model.transport.TransportAddress;
import com.optio3.protocol.model.transport.UdpTransportAddress;
import com.optio3.serialization.Reflection;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public final class UdpTransport extends AbstractTransport
{
    final static class BBMD
    {
        final InetSocketAddress address;
        final Integer           timeToLive;

        private ScheduledFuture<?> m_registerAsForeignDevice;

        BBMD(InetSocketAddress address,
             Integer timeToLive)
        {
            this.address    = address;
            this.timeToLive = timeToLive;
        }

        private void registerAsForeignDevice(UdpTransport transport)
        {
            if (m_registerAsForeignDevice != null)
            {
                m_registerAsForeignDevice.cancel(false);
                m_registerAsForeignDevice = null;
            }

            Integer timeToLive = this.timeToLive;
            if (timeToLive == null)
            {
                timeToLive = c_MinTimeToLive;
            }

            int timeToLiveActual = Math.min(timeToLive, c_MaxTimeToLive);

            transport.registerForeignDevice(address, timeToLiveActual);

            m_registerAsForeignDevice = Executors.scheduleOnDefaultPool(() -> registerAsForeignDevice(transport), timeToLiveActual / 2, TimeUnit.SECONDS);
        }

        private CompletableFuture<Void> unregisterAsForeignDevice(UdpTransport transport)
        {
            if (m_registerAsForeignDevice != null)
            {
                m_registerAsForeignDevice.cancel(false);
                m_registerAsForeignDevice = null;

                try
                {
                    List<ForeignDeviceTableEntry> table = await(transport.readForeignDeviceTable(address, 3, 200, TimeUnit.MILLISECONDS));
                    if (table != null)
                    {
                        for (ForeignDeviceTableEntry entry : table)
                        {
                            InetSocketAddress address = UdpTransportAddress.getSocketAddress(entry.device_address, entry.port);
                            transport.debug("ForeignDistributionTable: %s TTL:%s Timeout:%s", address, entry.time_to_live, entry.remaining_time_to_live);
                        }
                    }

                    //
                    // We should unregister, but in the presence of a NAT we don't know what address is registered.
                    // Instead, we re-register with a very short timeout.
                    //
                    transport.registerForeignDevice(address, 5);
                }
                catch (Throwable t)
                {
                    // Ignore failures.
                }
            }

            return wrapAsync(null);
        }
    }

    static final class Config
    {
        String  device;
        Integer serverPort;
        Integer networkPort;

        int getTargetPort()
        {
            if (serverPort != null)
            {
                return serverPort;
            }

            if (networkPort != null)
            {
                return networkPort;
            }

            return BACnetManager.c_DefaultPort;
        }
    }

    static final class CachedPacket
    {
        private static final Object       s_lock = new Object();
        private static       CachedPacket s_head;
        private static       int          s_count;

        final int c_maxCacheSize  = 64;
        final int c_MaxPacketSize = ConfirmedRequestPDU.MaxAPDU_1476 + 256;

        final byte[]         data      = new byte[c_MaxPacketSize];
        final DatagramPacket udpPacket = new DatagramPacket(data, c_MaxPacketSize);

        private CachedPacket m_next;

        static CachedPacket allocate()
        {
            synchronized (s_lock)
            {
                CachedPacket p = s_head;
                if (p != null)
                {
                    s_count--;
                    s_head   = p.m_next;
                    p.m_next = null;
                    return p;
                }

                return new CachedPacket();
            }
        }

        void recycle()
        {
            synchronized (s_lock)
            {
                if (s_count < c_maxCacheSize)
                {
                    m_next = s_head;
                    s_head = this;
                    s_count++;
                }
            }
        }
    }

    //--//

    private static final int c_MinTimeToLive = 120;
    private static final int c_MaxTimeToLive = 3600;

    private final Config     m_config;
    private final Object     m_lock            = new Object();
    private final List<BBMD> m_registeredBBMDs = Lists.newArrayList();
    private       boolean    m_enableBBMDs;

    private BACnetManager m_manager;

    private DatagramSocket m_socket;

    private InetAddress       m_localAddress;
    private InetSocketAddress m_localAddressWithPort;

    private InetAddress       m_maskAddress;
    private InetSocketAddress m_broadcastAddressWithPort;
    private boolean           m_lastSendFailed;

    private Thread m_receiveWorker;

    //--//

    UdpTransport(Config config)
    {
        m_config = config;
    }

    private void inferLocalAddress()
    {
        try
        {
            // If we are interested in a specific interface, list even the down ones.
            boolean includeDownInterfaces = (m_config.device != null);

            List<NetworkHelper.InterfaceAddressDetails> list = NetworkHelper.listNetworkAddresses(includeDownInterfaces, false, false, false, null);
            for (NetworkHelper.InterfaceAddressDetails details : list)
            {
                info("Itf: %s - %s %s", details.networkInterface.getName(), details.localAddress, details.maskAddress);
            }

            for (NetworkHelper.InterfaceAddressDetails details : list)
            {
                if (m_config.device != null && !m_config.device.equals(details.networkInterface.getName()))
                {
                    continue;
                }

                m_localAddress             = details.localAddress;
                m_maskAddress              = details.maskAddress;
                m_broadcastAddressWithPort = new InetSocketAddress(details.broadcastAddress, m_config.getTargetPort());
                return;
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        throw Exceptions.newRuntimeException("Failed to determine local address of host");
    }

    private DatagramSocket openSocket()
    {
        while (m_manager != null)
        {
            synchronized (m_lock)
            {
                try
                {
                    if (m_config.serverPort != null)
                    {
                        m_socket = new DatagramSocket(m_config.serverPort);
                    }
                    else if (m_config.networkPort != null)
                    {
                        m_socket = new DatagramSocket(m_config.networkPort);
                    }
                    else
                    {
                        m_socket = new DatagramSocket();
                    }

                    m_socket.setBroadcast(true);

                    inferLocalAddress();

                    m_localAddressWithPort = new InetSocketAddress(m_localAddress, m_socket.getLocalPort());
                    return m_socket;
                }
                catch (Throwable t)
                {
                    LoggerInstance.debug("Failed to open socket, due to %s", t);

                    TimeUtils.waitOnLock(m_lock, TimeUtils.computeTimeoutExpiration(5, TimeUnit.SECONDS));
                }
            }
        }

        return null;
    }

    public InetSocketAddress getLocalAddressWithPort()
    {
        return m_localAddressWithPort;
    }

    public InetAddress getMaskAddress()
    {
        return m_maskAddress;
    }

    public InetSocketAddress getBroadcastAddressWithPort()
    {
        return m_broadcastAddressWithPort;
    }

    //--//

    @Override
    public boolean equals(Object obj)
    {
        UdpTransport other = Reflection.as(obj, UdpTransport.class);
        if (other == null)
        {
            return false;
        }

        if (!StringUtils.equals(m_config.device, other.m_config.device))
        {
            return false;
        }

        if (!Objects.equals(m_config.serverPort, other.m_config.serverPort))
        {
            return false;
        }

        if (!Objects.equals(m_config.networkPort, other.m_config.networkPort))
        {
            return false;
        }

        return true;
    }

    @Override
    public void start(BACnetManager manager)
    {
        synchronized (m_lock)
        {
            if (m_manager == null)
            {
                m_manager = manager;

                m_receiveWorker = new Thread(this::worker);
                m_receiveWorker.setName("BACnet worker");
                m_receiveWorker.start();
            }
        }
    }

    @Override
    public void close()
    {
        DatagramSocket socket;
        Thread         receiveWorker;

        synchronized (m_lock)
        {
            socket        = m_socket;
            receiveWorker = m_receiveWorker;

            m_socket        = null;
            m_receiveWorker = null;
            m_manager       = null;

            m_lock.notifyAll();
        }

        if (socket != null)
        {
            socket.close();
        }

        if (receiveWorker != null)
        {
            try
            {
                receiveWorker.join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setSourceAddress(NetworkPDU npdu,
                                 Integer networkNumber)
    {
        if (m_enableBBMDs && !m_registeredBBMDs.isEmpty())
        {
            // We don't set the source address if going through a BBMD.
        }
        else
        {
            final UdpTransportAddress transportAddress = new UdpTransportAddress(m_localAddressWithPort);

            npdu.setSourceAddress(transportAddress.create(networkNumber));
        }
    }

    @Override
    public boolean canSend(TransportAddress destination)
    {
        return destination instanceof UdpTransportAddress;
    }

    @Override
    public int sendDirect(OutputBuffer ob,
                          TransportAddress destination)
    {
        //
        // Wrap in a BVLL header.
        //
        UdpTransportAddress dst  = (UdpTransportAddress) destination;
        OriginalUnicast     bvll = new OriginalUnicast();

        try (OutputBuffer bvllBuffer = bvll.encode(ob))
        {
            return sendInner(dst.socketAddress, bvllBuffer);
        }
    }

    @Override
    public void sendBroadcast(OutputBuffer ob)
    {
        if (m_enableBBMDs && !m_registeredBBMDs.isEmpty())
        {
            for (BBMD bbmd : m_registeredBBMDs)
            {
                DistributeBroadcastToNetwork bvll = new DistributeBroadcastToNetwork();

                try (OutputBuffer bvllBuffer = bvll.encode(ob))
                {
                    sendInner(bbmd.address, bvllBuffer);
                }
            }
        }
        else
        {
            OriginalBroadcast bvll = new OriginalBroadcast();

            try (OutputBuffer bvllBuffer = bvll.encode(ob))
            {
                sendInner(m_broadcastAddressWithPort, bvllBuffer);
            }
        }
    }

    @Override
    public boolean canReachAddress(InetAddress address)
    {
        NetworkHelper.NonRoutableRange routeTarget = NetworkHelper.isNonRoutableRange(address);
        if (routeTarget == null)
        {
            // Target is routable, no problem.
            return true;
        }

        //
        // Okay if we are in the same non-routable network.
        //
        NetworkHelper.NonRoutableRange routeOur = NetworkHelper.isNonRoutableRange(m_localAddress);
        return routeOur == routeTarget;
    }

    //--//

    @Override
    public synchronized void registerBBMD(TransportAddress ta)
    {
        UdpTransportAddress ta2 = Reflection.as(ta, UdpTransportAddress.class);
        if (ta2 != null)
        {
            for (BBMD bbmd : m_registeredBBMDs)
            {
                if (bbmd.address.equals(ta2.socketAddress))
                {
                    return;
                }
            }

            BBMD bbmd = new BBMD(ta2.socketAddress, null);
            m_registeredBBMDs.add(bbmd);
        }
    }

    @Override
    public void enableBBMDs()
    {
        for (BBMD bbmd : m_registeredBBMDs)
        {
            bbmd.registerAsForeignDevice(this);
        }

        m_enableBBMDs = true;
    }

    @Override
    public CompletableFuture<Void> disableBBMDs() throws
                                                  Exception
    {
        m_enableBBMDs = false;

        for (BBMD bbmd : m_registeredBBMDs)
        {
            await(bbmd.unregisterAsForeignDevice(this));
        }

        return wrapAsync(null);
    }

    //--//

    private int sendInner(InetSocketAddress destination,
                          OutputBuffer ob)
    {
        try
        {
            DatagramPacket p = new DatagramPacket(ob.toByteArray(), ob.size(), destination);
            debug("Sending UDP packet to '%s': %d", destination, p.getLength());
            dumpBuffer(LoggerInstance, Severity.DebugVerbose, ob.toByteArray(), ob.size());

            synchronized (m_lock)
            {
                if (m_socket == null)
                {
                    throw new ClosedChannelException();
                }

                m_socket.send(p);
                m_lastSendFailed = false;
            }

            return ob.size();
        }
        catch (Exception e)
        {
            Severity level;
            String   fmt;

            if (StringUtils.containsIgnoreCase(e.getMessage(), "unreachable"))
            {
                fmt   = "Can't send message to %s, port unreachable : %s";
                level = Severity.Debug;
            }
            else
            {
                fmt = "Received an exception trying to send message to %s : %s";

                if (!m_lastSendFailed)
                {
                    m_lastSendFailed = true;
                    level            = Severity.Info;
                }
                else
                {
                    level = Severity.Debug;
                }
            }

            log(null, level, null, null, fmt, destination, e);

            throw new RuntimeException(e);
        }
    }

    //--//

    public Integer getServerPort()
    {
        return m_config.serverPort;
    }

    public Integer getNetworkPort()
    {
        return m_config.networkPort;
    }

    //--//

    private void worker()
    {
        int            sleepBetweenFailures = 1;
        DatagramSocket socket               = openSocket();

        while (socket != null && !socket.isClosed())
        {
            CachedPacket p = CachedPacket.allocate();

            try
            {
                socket.receive(p.udpPacket);
            }
            catch (Exception e)
            {
                if (m_socket == null)
                {
                    // The manager has been stopped, exit.
                    return;
                }

                Executors.safeSleep(sleepBetweenFailures);

                // Exponential backoff if something goes wrong.
                sleepBetweenFailures = 2 * sleepBetweenFailures;
                continue;
            }

            sleepBetweenFailures = 1;

            Executors.getDefaultThreadPool()
                     .execute(() -> processPacket(p));
        }
    }

    private void processPacket(CachedPacket p)
    {
        InetSocketAddress source = new InetSocketAddress(p.udpPacket.getAddress(), p.udpPacket.getPort());
        int               length = p.udpPacket.getLength();

        try
        {
            debug("Received UDP packet from '%s': %d", source, length);
            dumpBuffer(LoggerInstance, Severity.DebugVerbose, p.data, length);

            try (InputBuffer ib = InputBuffer.createFrom(p.data, 0, length))
            {
                BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);

                ServiceContext sc = m_manager.allocateServiceContext(new UdpTransportAddress(source), length);

                bvll.dispatch(sc);
            }
        }
        catch (Exception e)
        {
            reportDecodeError(p.data, length, e);
        }
        finally
        {
            p.recycle();
        }
    }

    //--//

    int registerForeignDevice(InetSocketAddress address,
                              int timeToLiveActual)
    {
        RegisterForeignDevice reg = new RegisterForeignDevice();
        reg.timeToLive = Unsigned16.box(timeToLiveActual);

        return sendLinkRequest(reg, address);
    }

    public CompletableFuture<List<ForeignDeviceTableEntry>> readForeignDeviceTable(InetSocketAddress addressWithPort,
                                                                                   int retries,
                                                                                   int waitAmount,
                                                                                   TimeUnit unit) throws
                                                                                                  Exception
    {
        class State
        {
            CompletableFuture<List<ForeignDeviceTableEntry>> entries = new CompletableFuture<>();
        }

        State            state   = new State();
        InetAddress      address = addressWithPort.getAddress();
        TransportAddress ta      = new UdpTransportAddress(address.getHostAddress(), addressWithPort.getPort());

        try (LinkLayerListenerHandle<ReadForeignDeviceTable.Ack> listener = m_manager.registerLinkLayerListener(ReadForeignDeviceTable.Ack.class, (req, sc) ->
        {
            if (sc.source.equals(ta))
            {
                state.entries.complete(req.entries);
            }
        }))
        {
            for (int i = 0; i < retries; i++)
            {
                ReadForeignDeviceTable fdtRead = new ReadForeignDeviceTable();
                sendLinkRequest(fdtRead, addressWithPort);

                await(sleep(waitAmount, unit));

                if (state.entries.isDone())
                {
                    return state.entries;
                }
            }

            return wrapAsync(null);
        }
    }

    public CompletableFuture<List<BroadcastDistributionTableEntry>> readBroadcastDistributionTable(InetSocketAddress addressWithPort,
                                                                                                   int retries,
                                                                                                   int waitAmount,
                                                                                                   TimeUnit unit) throws
                                                                                                                  Exception
    {
        final CompletableFuture<List<BroadcastDistributionTableEntry>> entries = new CompletableFuture<>();

        InetAddress      address = addressWithPort.getAddress();
        TransportAddress ta      = new UdpTransportAddress(address.getHostAddress(), addressWithPort.getPort());

        try (LinkLayerListenerHandle<ReadBroadcastDistributionTable.Ack> listener = m_manager.registerLinkLayerListener(ReadBroadcastDistributionTable.Ack.class, (req, sc) ->
        {
            if (sc.source.equals(ta))
            {
                entries.complete(req.entries);
            }
        }))
        {
            for (int i = 0; i < retries; i++)
            {
                ReadBroadcastDistributionTable bdtRead = new ReadBroadcastDistributionTable();
                sendLinkRequest(bdtRead, addressWithPort);

                await(sleep(waitAmount, unit));

                if (entries.isDone())
                {
                    return entries;
                }
            }

            return wrapAsync(null);
        }
    }

    int sendLinkRequest(BaseVirtualLinkLayer request,
                        InetSocketAddress destination)
    {
        return sendLinkRequest(request, destination, NetworkPriority.Normal);
    }

    int sendLinkRequest(BaseVirtualLinkLayer request,
                        InetSocketAddress destination,
                        NetworkPriority priority)
    {
        Preconditions.checkNotNull(destination);

        try (OutputBuffer bvllBuffer = request.encode(null))
        {
            return sendInner(destination, bvllBuffer);
        }
    }
}
