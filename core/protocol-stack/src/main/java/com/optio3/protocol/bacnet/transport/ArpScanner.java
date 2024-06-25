/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.transport;

import java.net.InetAddress;
import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.interop.mediaaccess.EthernetAccess;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public final class ArpScanner
{
    private final String                   m_device;
    private final Map<InetAddress, byte[]> m_resolved = Maps.newHashMap();

    private EthernetAccess m_ethernet;
    private Thread         m_receiveWorker;
    private byte[]         m_itfHardware;
    private byte[]         m_itfProtocol;

    public ArpScanner(String device)
    {
        m_device = device;
    }

    //--//

    public void start()
    {
        String device = m_device;
        if (device == null)
        {
            device = EthernetAccess.lookupDev();
        }

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
                            m_itfHardware = ia.addr.data;
                            break;

                        case IpV4:
                            m_itfProtocol = ia.addr.data;
                            break;
                    }
                }
            }
        }

        if (m_itfProtocol == null)
        {
            m_itfProtocol = new byte[4];
        }

        m_ethernet = new EthernetAccess(device, 100);
        m_ethernet.setFilter("arp");

        m_receiveWorker = new Thread(() -> worker());
        m_receiveWorker.setName("ARP worker");
        m_receiveWorker.start();
    }

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

    public void send(InetAddress addr)
    {
        try (OutputBuffer outputBuffer = new OutputBuffer())
        {

            //  /*
            //   * Address Resolution Protocol.
            //   *
            //   * See RFC 826 for protocol description.  ARP packets are variable
            //   * in size; the arphdr structure defines the fixed-length portion.
            //   * Protocol type values are the same as those for 10 Mb/s Ethernet.
            //   * It is followed by the variable-sized fields ar_sha, arp_spa,
            //   * arp_tha and arp_tpa in that order, according to the lengths
            //   * specified.  Field names used correspond to RFC 826.
            //   */
            //  struct  arphdr
            //  {
            //      u_short ar_hrd;     /* format of hardware address */
            //  #define ARPHRD_ETHER    1   /* ethernet hardware format */
            //  #define ARPHRD_IEEE802  6   /* token-ring hardware format */
            //  #define ARPHRD_FRELAY   15  /* frame relay hardware format */
            //  #define ARPHRD_IEEE1394 24  /* IEEE1394 hardware address */
            //  #define ARPHRD_IEEE1394_EUI64 27 /* IEEE1394 EUI-64 */
            //      u_short ar_pro;     /* format of protocol address */
            //      u_char  ar_hln;     /* length of hardware address */
            //      u_char  ar_pln;     /* length of protocol address */
            //      u_short ar_op;      /* one of: */
            //  #define ARPOP_REQUEST   1   /* request to resolve address */
            //  #define ARPOP_REPLY 2   /* response to previous request */
            //  #define ARPOP_REVREQUEST 3  /* request protocol address given hardware */
            //  #define ARPOP_REVREPLY  4   /* response giving protocol address */
            //  #define ARPOP_INVREQUEST 8  /* request to identify peer */
            //  #define ARPOP_INVREPLY  9   /* response identifying peer */
            //  /*
            //   * The remaining fields are variable in size,
            //   * according to the sizes above.
            //   */
            //  #ifdef COMMENT_ONLY
            //      u_char  ar_sha[];   /* sender hardware address */
            //      u_char  ar_spa[];   /* sender protocol address */
            //      u_char  ar_tha[];   /* target hardware address */
            //      u_char  ar_tpa[];   /* target protocol address */
            //  #endif
            //  };

            for (int i = 0; i < 6; i++)
                 outputBuffer.emit1Byte(0xFF);

            outputBuffer.emit(m_itfHardware);

            outputBuffer.emit2Bytes(0x0806); // ARP Message

            outputBuffer.emit2Bytes(0x0001); // ar_hrd: ARPHRD_ETHER
            outputBuffer.emit2Bytes(0x0800); // ar_pro: EtherType
            outputBuffer.emit1Byte(0x06); // ar_hln: 6 for ethernet
            outputBuffer.emit1Byte(0x04); // ar_pln: 4 for IPv4
            outputBuffer.emit2Bytes(0x0001); // ar_op: ARPOP_REQUEST

            outputBuffer.emit(m_itfHardware); // ar_sha
            outputBuffer.emit(m_itfProtocol); // ar_spa
            outputBuffer.emit(new byte[6]); // ar_tha
            outputBuffer.emit(addr.getAddress()); // ar_tha

            m_ethernet.sendRaw(outputBuffer.toByteArray());
        }
    }

    public Map<InetAddress, byte[]> getResults()
    {
        synchronized (m_resolved)
        {
            return Maps.newHashMap(m_resolved);
        }
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
        try (InputBuffer inputBuffer = InputBuffer.createFrom(buf))
        {
            inputBuffer.readByteArray(6);
            inputBuffer.readByteArray(6);
            int messageType = inputBuffer.read2BytesUnsigned();
            if (messageType != 0x0806)
            {
                return;
            }

            int ar_hrd = inputBuffer.read2BytesUnsigned();
            if (ar_hrd != 0x0001)
            {
                return;
            }

            int ar_pro = inputBuffer.read2BytesUnsigned();
            if (ar_pro != 0x0800)
            {
                return;
            }

            int ar_hln = inputBuffer.read1ByteUnsigned();
            if (ar_hln != 0x06)
            {
                return;
            }

            int ar_pln = inputBuffer.read1ByteUnsigned();
            if (ar_pln != 0x04)
            {
                return;
            }

            int ar_op = inputBuffer.read2BytesUnsigned();
            if (ar_op != 2) // ARPOP_REPLY
            {
                return;
            }

            byte[]      srcMAC = inputBuffer.readByteArray(6);
            InetAddress srcIP  = InetAddress.getByAddress(inputBuffer.readByteArray(4));

            synchronized (m_resolved)
            {
                m_resolved.put(srcIP, srcMAC);
            }
        }
        catch (Exception e)
        {
            // Ignore exceptions.
        }
    }
}
