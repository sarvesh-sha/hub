/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.bacnet;

import static com.optio3.util.Exceptions.getAndUnwrapException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.optio3.interop.mediaaccess.EthernetAccess;
import com.optio3.interop.mediaaccess.SerialAccess;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.bacnet.BACnetManager;
import com.optio3.protocol.bacnet.DeviceIdentity;
import com.optio3.protocol.bacnet.model.pdu.TagContextCommon;
import com.optio3.protocol.bacnet.model.pdu.TagHeaderCommon;
import com.optio3.protocol.bacnet.model.pdu.request.UnconfirmedServiceRequest;
import com.optio3.protocol.bacnet.transport.AbstractTransport;
import com.optio3.protocol.bacnet.transport.ArpScanner;
import com.optio3.protocol.bacnet.transport.UdpTransport;
import com.optio3.protocol.bacnet.transport.UdpTransportBuilder;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.objects.analog_output;
import com.optio3.protocol.model.transport.UdpTransportAddress;
import com.optio3.stream.InputBuffer;
import com.optio3.test.common.Optio3Test;
import com.optio3.util.CollectionUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class BACnetManagerTest extends Optio3Test
{
    public static boolean verbose        = false;
    public static Logger  LoggerInstance = new Logger(BACnetManagerTest.class);

    @BeforeClass
    public static void setVerboseLogging()
    {
        if (verbose)
        {
            TagContextCommon.LoggerInstance.enable(Severity.Debug);
            TagHeaderCommon.LoggerInstance.enable(Severity.Debug);
            InputBuffer.LoggerInstance.enable(Severity.Debug);
        }
    }

    //--//

    @Ignore
    @Test
    public void manager() throws
                          InterruptedException
    {
        UdpTransportBuilder transportBuilder = UdpTransportBuilder.newBuilder();
        transportBuilder.setServerPort(BACnetManager.c_DefaultPort);
        AbstractTransport transport = transportBuilder.build();

        BACnetManager mgr = new BACnetManager();
        mgr.addTransport(transport);
        mgr.setInstanceNumber(2345);
        mgr.setVendorId(1);

        mgr.registerListener(UnconfirmedServiceRequest.class, (msg, sc) ->
        {
            LoggerInstance.info("Test Got %s from %s", msg.getClass(), sc.source);
        });

        mgr.start();

        Thread.sleep(100000);
        mgr.close();
    }

    @Ignore
    @Test
    public void managerTestWrite() throws
                                   Exception
    {
        final boolean targetLab = true;
        final String  bbmdAddr;
        final int     bbmdPort;

        if (targetLab)
        {
            bbmdAddr = "ldap.dev.optio3.io";
            bbmdPort = 0xBAC0;
        }
        else
        {
            bbmdAddr = "127.0.0.1";
            bbmdPort = 0xBAC0;
        }

        LoggerInstance.info("%s", bbmdAddr);
        InetSocketAddress bbmd = new InetSocketAddress(InetAddress.getByName(bbmdAddr), bbmdPort);

        UdpTransportBuilder transportBuilder = UdpTransportBuilder.newBuilder();
        transportBuilder.setNetworkPort(0xBAC2);
        AbstractTransport transport = transportBuilder.build();

        BACnetManager mgr = new BACnetManager();
        mgr.addTransport(transport);
        mgr.setDefaultRetries(3);
        mgr.setDefaultTimeout(Duration.ofSeconds(1));

        mgr.addBBMD(new UdpTransportAddress(bbmd));

        mgr.start();

        Set<DeviceIdentity> broadcastDevices = getAndUnwrapException(mgr.scanForDevices(LoggerInstance, 3, null, false, false, null, (msg, di) ->
        {
            LoggerInstance.debug("  >> Got IAm from %s at %s", msg.i_am_device_identifier, di.getTransportAddress());
        }));

        DeviceIdentity di = CollectionUtils.firstElement(broadcastDevices);
        //--//

        var objects = getAndUnwrapException(di.getObjects());

        LoggerInstance.info("Got %d objects", objects.size());
        for (DeviceIdentity.ObjectDescriptor objDesc : objects.values())
        {
            if (objDesc.id.object_type == BACnetObjectType.analog_output.forRequest())
            {
                LoggerInstance.info("Reading %s", objDesc.id);
                Set<BACnetPropertyIdentifierOrUnknown> set = getAndUnwrapException(objDesc.getProperties());
                LoggerInstance.info("Got %d properties", set.size());

                DeviceIdentity.BatchReader reader = di.createBatchReader();

                for (BACnetPropertyIdentifierOrUnknown prop : set)
                {
                    reader.add(objDesc, prop);
                }

                DeviceIdentity.BatchReaderResult output = getAndUnwrapException(reader.execute(mgr.getDefaultTimeout()));
                LoggerInstance.info("Read %d properties", output.values.size());

                var resBefore = getAndUnwrapException(di.readProperty(mgr.getDefaultTimeout(), objDesc.id, BACnetPropertyIdentifier.present_value.forRequest(), analog_output.class));
                LoggerInstance.info("Before: %s %s", objDesc.id, resBefore.present_value);

                var res = getAndUnwrapException(di.writeProperty(mgr.getDefaultTimeout(), objDesc.id, BACnetPropertyIdentifier.present_value.forRequest(), resBefore.present_value + 1.0f));
                LoggerInstance.info("Write: %s", res);

                var resAfter = getAndUnwrapException(di.readProperty(mgr.getDefaultTimeout(), objDesc.id, BACnetPropertyIdentifier.present_value.forRequest(), analog_output.class));
                LoggerInstance.info("After: %s %s", objDesc.id, resAfter.present_value);
            }
        }

        mgr.close();
    }

    @Ignore
    @Test
    public void testPcap()
    {
        String dev = EthernetAccess.lookupDev();
        System.out.println(dev);

        String device = EthernetAccess.lookupDev();

        EthernetAccess pcap = new EthernetAccess(device, 100);

        pcap.setFilter("ether[0] == 0x98");

        pcap.loop(10, (payload) ->
        {
            System.out.printf("Got packet %d%n", payload.length);
            byte[] outpacket = new byte[14 + 0x17];
            System.arraycopy(payload, 6, outpacket, 0, 6);
            System.arraycopy(payload, 0, outpacket, 6, 6);
            outpacket[12] = 0;
            outpacket[13] = (byte) 0x17;
            outpacket[14] = (byte) 0x82;
            outpacket[15] = (byte) 0x82;
            outpacket[16] = (byte) 0x03;

            outpacket[17 + 0x00] = (byte) 0x01;
            outpacket[17 + 0x01] = (byte) 0x20;
            outpacket[17 + 0x02] = (byte) 0xff;
            outpacket[17 + 0x03] = (byte) 0xff;
            outpacket[17 + 0x04] = (byte) 0x00;
            outpacket[17 + 0x05] = (byte) 0xff;
            outpacket[17 + 0x06] = (byte) 0x10;
            outpacket[17 + 0x07] = (byte) 0x00;
            outpacket[17 + 0x08] = (byte) 0xc4;
            outpacket[17 + 0x09] = (byte) 0x02;
            outpacket[17 + 0x0A] = (byte) 0x00;
            outpacket[17 + 0x0B] = (byte) 0x42;
            outpacket[17 + 0x0C] = (byte) 0x68;
            outpacket[17 + 0x0D] = (byte) 0x22;
            outpacket[17 + 0x0E] = (byte) 0x05;
            outpacket[17 + 0x0F] = (byte) 0xc4;
            outpacket[17 + 0x10] = (byte) 0x91;
            outpacket[17 + 0x11] = (byte) 0x00;
            outpacket[17 + 0x12] = (byte) 0x21;
            outpacket[17 + 0x13] = (byte) 0x12;
            pcap.sendRaw(outpacket);
        });
    }

    @Ignore
    @Test
    public void testInterfaces()
    {
        List<EthernetAccess.InterfaceDescriptor> lst = EthernetAccess.findAllDevices();
        for (EthernetAccess.InterfaceDescriptor id : lst)
        {
            System.out.printf("ID: %s%n", id.name);
            for (EthernetAccess.InterfaceAddress ia : id.addresses)
            {
                System.out.printf("addr     : %s%n", ia.addr);
                System.out.printf("broadaddr: %s%n", ia.broadaddr);
                System.out.printf("netmask  : %s%n", ia.netmask);
                System.out.printf("dstaddr  : %s%n", ia.dstaddr);
            }
        }
    }

    @Ignore
    @Test
    public void testSerial()
    {
        SerialAccess sa = new SerialAccess("/dev/cu.usbserial-A600NE4X", 38400, 8, 'N', 1);
        sa.write("Test");
        sa.close();
    }

    @Ignore
    @Test
    public void testArpScanner() throws
                                 UnknownHostException,
                                 InterruptedException
    {
        ArpScanner scanner = new ArpScanner("en0");

        scanner.start();

        for (int pass = 0; pass < 10; pass++)
        {
            for (int i = 1; i < 254; i++)
            {
                final InetAddress addr = InetAddress.getByName("192.168.1." + i);
                if (!scanner.getResults()
                            .containsKey(addr))
                {
                    scanner.send(addr);
                }
            }
            Thread.sleep(100);
        }
        Thread.sleep(2000);

        final Map<InetAddress, byte[]> results = scanner.getResults();
        for (InetAddress addr : results.keySet())
        {
            System.out.printf("Found %s%n", addr);
        }

        scanner.close();
    }

    @Ignore
    @Test
    public void testRouterDiscovery() throws
                                      Exception
    {
        UdpTransportBuilder transportBuilder = UdpTransportBuilder.newBuilder();

        transportBuilder.setNetworkPort(0xBAC0);

        UdpTransport transport = transportBuilder.build();

        BACnetManager mgr = new BACnetManager();
        mgr.addTransport(transport);

        mgr.start();
        getAndUnwrapException(mgr.scanForRouters(20, (msg, di) ->
        {
            System.out.printf("  >> Got %s%n", msg.networks);
        }, 2, TimeUnit.SECONDS));

        mgr.close();
    }
}
