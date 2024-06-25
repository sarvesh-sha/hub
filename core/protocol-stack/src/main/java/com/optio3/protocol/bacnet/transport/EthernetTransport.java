/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.transport;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.interop.mediaaccess.EthernetAccess;
import com.optio3.logging.Severity;
import com.optio3.protocol.bacnet.BACnetManager;
import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.protocol.bacnet.model.pdu.NetworkPDU;
import com.optio3.protocol.model.transport.EthernetTransportAddress;
import com.optio3.protocol.model.transport.TransportAddress;
import com.optio3.serialization.Reflection;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import org.apache.commons.lang3.StringUtils;

public final class EthernetTransport extends AbstractTransport
{
    static final class Config
    {
        String device;
    }

    //--//

    private final Config m_config;

    private BACnetManager m_manager;

    private EthernetAccess   m_ethernet;
    private TransportAddress m_localAddress;

    private Thread m_receiveWorker;

    EthernetTransport(Config config)
    {
        m_config = config;
    }

    //--//

    @Override
    public boolean equals(Object obj)
    {
        EthernetTransport other = Reflection.as(obj, EthernetTransport.class);
        if (other == null)
        {
            return false;
        }

        if (!StringUtils.equals(m_config.device, other.m_config.device))
        {
            return false;
        }

        return true;
    }

    @Override
    public void start(BACnetManager manager)
    {
        if (m_manager == null)
        {
            String device = m_config.device;
            if (device == null)
            {
                device = EthernetAccess.lookupDev();
            }

            m_ethernet = new EthernetAccess(device, 100);
            m_ethernet.setFilter("llc and ether[14] = 0x82 and ether[15] = 0x82 and ether[16] = 0x3");

            m_localAddress = extractLinkAddress(device);

            m_manager = manager;

            m_receiveWorker = new Thread(this::worker);
            m_receiveWorker.setName("BACnet worker");
            m_receiveWorker.start();
        }
    }

    private static TransportAddress extractLinkAddress(String device)
    {
        for (EthernetAccess.InterfaceDescriptor id : EthernetAccess.findAllDevices())
        {
            if (id.name.equals(device))
            {
                for (EthernetAccess.InterfaceAddress ia : id.addresses)
                {
                    if (ia.addr == null)
                    {
                        continue;
                    }

                    switch (ia.addr.family)
                    {
                        case Link:
                        case Packet:
                            return new EthernetTransportAddress(ia.addr.data);

                        default:
                            break;
                    }
                }
            }
        }

        return null;
    }

    @Override
    public void close()
    {
        EthernetAccess ethernet;
        Thread         receiveWorker;

        synchronized (this)
        {
            ethernet      = m_ethernet;
            receiveWorker = m_receiveWorker;

            m_ethernet      = null;
            m_receiveWorker = null;
        }

        if (ethernet != null)
        {
            ethernet.close();
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
        npdu.setSourceAddress(m_localAddress.create(networkNumber));
    }

    @Override
    public boolean canSend(TransportAddress destination)
    {
        return destination instanceof EthernetTransportAddress;
    }

    @Override
    public int sendDirect(OutputBuffer ob,
                          TransportAddress destination)
    {
        return sendInner(destination, ob);
    }

    @Override
    public void sendBroadcast(OutputBuffer ob)
    {
        sendInner(null, ob);
    }

    private int sendInner(TransportAddress destination,
                          OutputBuffer ob)
    {
        try (OutputBuffer llcBuffer = new OutputBuffer())
        {

            //
            // Wrap in a LLC header.
            //
            if (destination != null)
            {
                llcBuffer.emit(destination.asBytes());
            }
            else
            {
                for (int i = 0; i < 6; i++)
                {
                    llcBuffer.emit1Byte(0xFF);
                }
            }

            llcBuffer.emit(m_localAddress.asBytes());
            llcBuffer.emit2Bytes(3 + ob.size());

            llcBuffer.emit1Byte(0x82);
            llcBuffer.emit1Byte(0x82);
            llcBuffer.emit1Byte(0x03);
            llcBuffer.emitNestedBlock(ob);

            CompletableFuture<Integer> res = new CompletableFuture<>();

            try
            {
                debug("Sending ETHER packet to '%s': %d", destination, llcBuffer.size());
                dumpBuffer(LoggerInstance, Severity.DebugVerbose, ob.toByteArray(), ob.size());

                synchronized (this)
                {
                    m_ethernet.sendRaw(llcBuffer.toByteArray());
                }

                return llcBuffer.size();
            }
            catch (Exception e)
            {
                error("Received an exception trying to send message to %s: %s", destination, e);
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean canReachAddress(InetAddress address)
    {
        // This transport doesn't do IP routing.
        return false;
    }

    //--//

    @Override
    public void registerBBMD(TransportAddress ta)
    {
        // This transport doesn't do IP routing.
    }

    @Override
    public void enableBBMDs()
    {
        // This transport doesn't do IP routing.
    }

    @Override
    public CompletableFuture<Void> disableBBMDs()
    {
        // This transport doesn't do IP routing.
        return AsyncRuntime.NullResult;
    }

    //--//

    private void worker()
    {
        EthernetAccess ethernet = m_ethernet;

        while (!ethernet.isClosed())
        {
            ethernet.loop(1, this::processPacket);
        }
    }

    private void processPacket(byte[] buf)
    {
        int length = buf.length;

        debug("Received ETHER packet: %d", length);
        dumpBuffer(LoggerInstance, Severity.DebugVerbose, buf, length);

        try (InputBuffer ib = InputBuffer.createFrom(buf))
        {
            @SuppressWarnings("unused") byte[] dst        = ib.readByteArray(6);
            byte[]                             src        = ib.readByteArray(6);
            int                                payloadLen = ib.read2BytesUnsigned();

            // @formatter:off
            int dsap = ib.read1ByteUnsigned(); if (dsap != 0x82) return;
            int ssap = ib.read1ByteUnsigned(); if (ssap != 0x82) return;
            int ctrl = ib.read1ByteUnsigned(); if (ctrl != 0x03) return;
            // @formatter:on

            InputBuffer payload = ib.readNestedBlock(payloadLen - 3);

            ServiceContext sc = m_manager.allocateServiceContext(new EthernetTransportAddress(src), length);

            try (NetworkPDU npdu = NetworkPDU.decode(payload))
            {
                sc.processNetworkRequest(npdu);
            }
        }
        catch (Exception e)
        {
            reportDecodeError(buf, length, e);
        }
    }
}
