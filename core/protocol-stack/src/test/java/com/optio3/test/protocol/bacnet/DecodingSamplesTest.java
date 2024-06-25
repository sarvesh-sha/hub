/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.bacnet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.lang.Unsigned32;
import com.optio3.lang.Unsigned8;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.LoggerResource;
import com.optio3.logging.Severity;
import com.optio3.protocol.bacnet.model.enums.ConfirmedServiceChoice;
import com.optio3.protocol.bacnet.model.enums.UnconfirmedServiceChoice;
import com.optio3.protocol.bacnet.model.linklayer.BaseVirtualLinkLayer;
import com.optio3.protocol.bacnet.model.linklayer.DistributeBroadcastToNetwork;
import com.optio3.protocol.bacnet.model.linklayer.Forwarded;
import com.optio3.protocol.bacnet.model.linklayer.NetworkPayload;
import com.optio3.protocol.bacnet.model.linklayer.OriginalBroadcast;
import com.optio3.protocol.bacnet.model.linklayer.OriginalUnicast;
import com.optio3.protocol.bacnet.model.linklayer.ReadBroadcastDistributionTable;
import com.optio3.protocol.bacnet.model.linklayer.Result;
import com.optio3.protocol.bacnet.model.pdu.NetworkPDU;
import com.optio3.protocol.bacnet.model.pdu.TagContextCommon;
import com.optio3.protocol.bacnet.model.pdu.TagContextForDecoding;
import com.optio3.protocol.bacnet.model.pdu.TagContextForEncoding;
import com.optio3.protocol.bacnet.model.pdu.TagHeaderCommon;
import com.optio3.protocol.bacnet.model.pdu.application.ApplicationPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ComplexAckPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ConfirmedRequestPDU;
import com.optio3.protocol.bacnet.model.pdu.application.UnconfirmedRequestPDU;
import com.optio3.protocol.bacnet.model.pdu.network.IAmRouterToNetworkPDU;
import com.optio3.protocol.bacnet.model.pdu.network.NetworkMessagePDU;
import com.optio3.protocol.bacnet.model.pdu.network.WhoIsRouterToNetworkPDU;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ConfirmedCOVNotification;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.CreateObject;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ReadProperty;
import com.optio3.protocol.bacnet.model.pdu.request.confirmed.ReadPropertyMultiple;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.IAm;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.TimeSynchronization;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.UnconfirmedEventNotification;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.WhoHas;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.WhoIs;
import com.optio3.protocol.bacnet.model.pdu.request.unconfirmed.WriteGroup;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.constructed.BACnetAddressBinding;
import com.optio3.protocol.model.bacnet.constructed.BACnetDestination;
import com.optio3.protocol.model.bacnet.constructed.BACnetPriorityArray;
import com.optio3.protocol.model.bacnet.constructed.BACnetPropertyValue;
import com.optio3.protocol.model.bacnet.constructed.ReadAccessResult;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetEventParameter;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetNotificationParameters;
import com.optio3.protocol.model.bacnet.enums.BACnetFileAccessMethod;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.enums.BACnetSegmentation;
import com.optio3.protocol.model.bacnet.enums.BACnetVirtualLinkLayerResult;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetServicesSupported.Values;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetStatusFlags;
import com.optio3.protocol.model.bacnet.objects.analog_input;
import com.optio3.protocol.model.bacnet.objects.device;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.test.common.AutoRetryOnFailure;
import com.optio3.test.common.Optio3Test;
import com.optio3.util.BufferUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DecodingSamplesTest extends Optio3Test
{
    private static final String c_epics_run = "BACnet/EpicsRun.txt";

    public static boolean verboseSweep = false;

    private boolean m_verbose;

    @Before
    public void setVerboseLogging()
    {
        if (failedOnFirstRun())
        {
            m_verbose = true;

            TagContextCommon.LoggerInstance.enablePerThread(Severity.Debug);
            TagHeaderCommon.LoggerInstance.enablePerThread(Severity.Debug);
            InputBuffer.LoggerInstance.enablePerThread(Severity.Debug);
            OutputBuffer.LoggerInstance.enablePerThread(Severity.Debug);
        }
        else
        {
            m_verbose = false;
        }
    }

    @After
    public void resetVerboseLogging()
    {
        TagContextCommon.LoggerInstance.inheritPerThread(Severity.Debug);
        TagHeaderCommon.LoggerInstance.inheritPerThread(Severity.Debug);
        InputBuffer.LoggerInstance.inheritPerThread(Severity.Debug);
        OutputBuffer.LoggerInstance.inheritPerThread(Severity.Debug);
    }

    //--//

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void networkMessage_WhoIsRouterToNetworkPDU()
    {
        byte[] buf = parseHex(new String[] { "81 0a 00 09", "01 80 00 12 34" });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        OriginalUnicast      msg  = assertCast(OriginalUnicast.class, bvll);

        NetworkPDU npdu = msg.decodePayload();
        assertTrue(npdu.network_message);

        NetworkMessagePDU       pdu  = npdu.decodeNetworkMessageHeader();
        WhoIsRouterToNetworkPDU res2 = assertCast(WhoIsRouterToNetworkPDU.class, pdu);
        assertNotNull(res2);
        assertTrue(res2.networkNumber.isPresent());
        assertUnsignedEquals(0x1234, res2.networkNumber.get());

        //--//

        checkEncodingOfNetworkMessage(buf, npdu, pdu);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void networkMessage_WhoIsRouterToNetworkPDU_2()
    {
        byte[] buf = parseHex(new String[] { "81 0a 00 07", "01 80 00" });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        OriginalUnicast      msg  = assertCast(OriginalUnicast.class, bvll);

        NetworkPDU npdu = msg.decodePayload();
        assertTrue(npdu.network_message);

        NetworkMessagePDU       pdu  = npdu.decodeNetworkMessageHeader();
        WhoIsRouterToNetworkPDU res2 = assertCast(WhoIsRouterToNetworkPDU.class, pdu);
        assertNotNull(res2);
        assertFalse(res2.networkNumber.isPresent());

        //--//

        checkEncodingOfNetworkMessage(buf, npdu, pdu);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void networkMessage_IAmRouterToNetworkPDU()
    {
        byte[] buf = parseHex(new String[] { "81 0a 00 0B", "01 80 01 12 34 56 78" });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        OriginalUnicast      msg  = assertCast(OriginalUnicast.class, bvll);

        NetworkPDU npdu = msg.decodePayload();
        assertTrue(npdu.network_message);

        NetworkMessagePDU     pdu  = npdu.decodeNetworkMessageHeader();
        IAmRouterToNetworkPDU res2 = assertCast(IAmRouterToNetworkPDU.class, pdu);
        assertNotNull(res2);
        assertEquals(2, res2.networks.size());

        //--//

        checkEncodingOfNetworkMessage(buf, npdu, pdu);
    }

    //--//

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_WhoIs()
    {
        byte[] buf = parseHex(new String[] { "81 09 00 0c 01 20 ff ff 00 ff 10 08" });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer         bvll = BaseVirtualLinkLayer.decode(ib);
        DistributeBroadcastToNetwork msg  = assertCast(DistributeBroadcastToNetwork.class, bvll);

        NetworkPDU npdu = msg.decodePayload();

        assertTrue(npdu.destination_specifier);
        assertFalse(npdu.source_specifier);
        assertUnsignedEquals(0xFFFF, npdu.dnet);

        ApplicationPDU pdu  = npdu.decodeApplicationHeader();
        Object         res  = pdu.decodePayload();
        WhoIs          res2 = assertCast(WhoIs.class, res);
        assertNotNull(res2);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_WhoHas()
    {
        byte[] buf = parseHex(new String[] { "81 04 00 69 0A 6F 50 03 BA C0 01 00 10 07 3D 59",
                                             "04 00 53 00 6B 00 79 00 76 00 69 00 65 00 77 00",
                                             "2F 00 53 00 63 00 68 00 65 00 64 00 75 00 6C 00",
                                             "65 00 2E 00 53 00 6B 00 79 00 76 00 69 00 65 00",
                                             "77 00 20 00 43 00 6C 00 61 00 73 00 73 00 72 00",
                                             "6F 00 6F 00 6D 00 73 00 20 00 43 00 61 00 6C 00",
                                             "65 00 6E 00 64 00 61 00 72" });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        Forwarded            msg  = assertCast(Forwarded.class, bvll);

        NetworkPDU npdu = msg.decodePayload();

        ApplicationPDU pdu  = npdu.decodeApplicationHeader();
        Object         res  = pdu.decodePayload();
        WhoHas         res2 = assertCast(WhoHas.class, res);
        assertNotNull(res2);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_TimeSynchronization()
    {
        byte[] buf = parseHex(new String[] { "81 04 00 1C AC 10 06 29 BA C4 01 20 FF FF 00 FF", "10 06 A4 76 04 08 07 B4 00 00 00 1F" });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        Forwarded            msg  = assertCast(Forwarded.class, bvll);

        NetworkPDU npdu = msg.decodePayload();

        ApplicationPDU      pdu  = npdu.decodeApplicationHeader();
        Object              res  = pdu.decodePayload();
        TimeSynchronization res2 = assertCast(TimeSynchronization.class, res);
        assertNotNull(res2);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_BvlcResult()
    {
        byte[] buf = parseHex(new String[] { "81 00 00 06 00 30", });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        Result               msg  = assertCast(Result.class, bvll);

        assertEquals(BACnetVirtualLinkLayerResult.Register_Foreign_Device_NAK, msg.result);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_IAm()
    {
        byte[] buf = parseHex(new String[] { "81 0b 00 19 01 20 ff ff 00 ff 10 00 c4 02 00 04", "d2 22 05 c4 91 03 22 01 04", });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        OriginalBroadcast    msg  = assertCast(OriginalBroadcast.class, bvll);

        NetworkPDU npdu = msg.decodePayload();
        assertTrue(npdu.destination_specifier);
        assertFalse(npdu.source_specifier);
        assertUnsignedEquals(0xFFFF, npdu.dnet);

        ApplicationPDU pdu  = npdu.decodeApplicationHeader();
        Object         res  = pdu.decodePayload();
        IAm            res2 = assertCast(IAm.class, res);
        assertNotNull(res2);

        assertEquals(BACnetObjectType.device, res2.i_am_device_identifier.object_type.value);
        assertUnsignedEquals(1234, res2.i_am_device_identifier.instance_number);
        assertUnsignedEquals(1476, res2.max_apdu_length_accepted);
        assertEquals(BACnetSegmentation.no_segmentation, res2.segmentation_supported);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_readProperty()
    {
        byte[] buf = parseHex(new String[] { "81 0a 00 11 01 04 00 05 01 0c 0c 02 00 04 d2 19", "4b", });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        OriginalUnicast      msg  = assertCast(OriginalUnicast.class, bvll);

        NetworkPDU npdu = msg.decodePayload();

        assertFalse(npdu.destination_specifier);
        assertFalse(npdu.source_specifier);
        assertTrue(npdu.data_expecting_reply);

        ApplicationPDU pdu  = npdu.decodeApplicationHeader();
        Object         res  = pdu.decodePayload();
        ReadProperty   res2 = assertCast(ReadProperty.class, res);
        assertNotNull(res2);

        assertEquals(BACnetObjectType.device, res2.object_identifier.object_type.value);
        assertUnsignedEquals(1234, res2.object_identifier.instance_number);
        assertEquals(BACnetPropertyIdentifier.object_identifier, res2.property_identifier.value);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_readPropertyAck()
    {
        byte[] buf = parseHex(new String[] { "81 0A 00 1C 01 00 30 06 0C 0C 00 C0 00 77 1A 01", "63 3E 0C 00 FF FF FF 1B 3F FF FF 3F", });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        OriginalUnicast      msg  = assertCast(OriginalUnicast.class, bvll);

        NetworkPDU npdu = msg.decodePayload();

        ApplicationPDU   pdu  = npdu.decodeApplicationHeader();
        Object           res  = pdu.decodePayload();
        ReadProperty.Ack res2 = assertCast(ReadProperty.Ack.class, res);
        assertNotNull(res2);

        assertEquals(BACnetObjectType.binary_input, res2.object_identifier.object_type.value);
        assertUnsignedEquals(119, res2.object_identifier.instance_number);
        assertEquals(BACnetPropertyIdentifier.event_algorithm_inhibit_ref, res2.property_identifier.value);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_readDeviceBinding()
    {
        byte[] buf = parseHex(new String[] { "81 0a 01 6b 01 00",
                                             "30 15 0c 0c 02 00 00 00 19 1e 3e c4 02 00 15 7d",
                                             "21 00 61 01 c4 02 00 15 7e 21 00 61 02 c4 02 00",
                                             "15 7f 21 00 61 03 c4 02 00 15 80 21 00 61 04 c4",
                                             "02 00 15 81 21 00 61 05 c4 02 00 15 82 21 00 61",
                                             "06 c4 02 00 15 83 21 00 61 07 c4 02 00 15 84 21",
                                             "00 61 08 c4 02 00 15 85 21 00 61 09 c4 02 00 15",
                                             "86 21 00 61 0a c4 02 00 15 87 21 00 61 0b c4 02",
                                             "00 15 88 21 00 61 0c c4 02 00 15 89 21 00 61 0d",
                                             "c4 02 00 15 8a 21 00 61 0e c4 02 00 15 8b 21 00",
                                             "61 0f c4 02 00 15 8c 21 00 61 10 c4 02 00 15 8d",
                                             "21 00 61 11 c4 02 00 15 8e 21 00 61 12 c4 02 00",
                                             "15 8f 21 00 61 13 c4 02 00 15 90 21 00 61 14 c4",
                                             "02 00 15 91 21 00 61 15 c4 02 00 15 92 21 00 61",
                                             "16 c4 02 00 15 93 21 00 61 17 c4 02 00 15 94 21",
                                             "00 61 18 c4 02 00 15 95 21 00 61 19 c4 02 00 15",
                                             "96 21 00 61 1a c4 02 00 15 97 21 00 61 1b c4 02",
                                             "00 15 98 21 00 61 1c c4 02 00 15 99 21 00 61 1d",
                                             "c4 02 00 15 9a 21 00 61 1e c4 02 00 15 9b 21 00",
                                             "61 1f c4 02 00 15 9c 21 00 61 20 c4 02 00 15 9d",
                                             "21 00 61 21 c4 02 00 15 9e 21 00 61 22 c4 02 00",
                                             "15 9f 21 00 61 23 c4 02 00 15 a0 22 13 88 61 24",
                                             "c4 02 00 d7 9f 22 13 88 61 2c c4 02 00 d8 17 22",
                                             "13 88 61 2d 3f" });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        OriginalUnicast      msg  = assertCast(OriginalUnicast.class, bvll);

        NetworkPDU npdu = msg.decodePayload();

        assertFalse(npdu.destination_specifier);
        assertFalse(npdu.source_specifier);
        assertFalse(npdu.data_expecting_reply);

        ApplicationPDU   pdu  = npdu.decodeApplicationHeader();
        Object           res  = pdu.decodePayload();
        ReadProperty.Ack res2 = assertCast(ReadProperty.Ack.class, res);
        assertNotNull(res2);

        assertEquals(BACnetObjectType.device, res2.object_identifier.object_type.value);
        assertUnsignedEquals(0, res2.object_identifier.instance_number);
        assertEquals(BACnetPropertyIdentifier.device_address_binding, res2.property_identifier.value);
        @SuppressWarnings("unchecked") ArrayList<BACnetAddressBinding> val = assertCast(ArrayList.class, res2.property_value);
        assertEquals(38, val.size());

        device obj = res2.toObject(device.class);
        assertUnsignedEquals(5501, obj.device_address_binding.get(0).device_identifier.instance_number);
        assertUnsignedEquals(55319, obj.device_address_binding.get(37).device_identifier.instance_number);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_readPropertyResponse()
    {
        byte[] buf = parseHex(new String[] { "81 0a 00 17 01 00 30 01 0c 0c 02 00 04 d2 19 4b", "3e c4 02 00 04 d2 3f" });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        OriginalUnicast      msg  = assertCast(OriginalUnicast.class, bvll);

        NetworkPDU npdu = msg.decodePayload();

        assertFalse(npdu.destination_specifier);
        assertFalse(npdu.source_specifier);
        assertFalse(npdu.data_expecting_reply);

        ApplicationPDU   pdu  = npdu.decodeApplicationHeader();
        Object           res  = pdu.decodePayload();
        ReadProperty.Ack res2 = assertCast(ReadProperty.Ack.class, res);
        assertNotNull(res2);

        assertEquals(BACnetObjectType.device, res2.object_identifier.object_type.value);
        assertUnsignedEquals(1234, res2.object_identifier.instance_number);
        assertEquals(BACnetPropertyIdentifier.object_identifier, res2.property_identifier.value);
        BACnetObjectIdentifier val = assertCast(BACnetObjectIdentifier.class, res2.property_value);
        assertEquals(BACnetObjectType.device, val.object_type.value);
        assertUnsignedEquals(1234, val.instance_number);

        device obj = res2.toObject(device.class);
        assertUnsignedEquals(1234, obj.object_identifier.instance_number);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_ReadBroadcastDistributionTable()
    {
        byte[] buf = parseHex(new String[] { "81 03 00 22", "01 23 7f 00 00 01 00 00 00 01", "01 23 7f 00 00 02 00 00 01 00", "01 23 7f 00 00 03 00 01 00 00", });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer               bvll = BaseVirtualLinkLayer.decode(ib);
        ReadBroadcastDistributionTable.Ack msg  = assertCast(ReadBroadcastDistributionTable.Ack.class, bvll);
        assertEquals(3, msg.entries.size());
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_ReadPropertyMultipleResponse()
    {
        byte[] buf = parseHex(new String[] { "81 0a 02 0d 01 00 30 01 0e 0c 02 00 04 d2 1e 29 4b 4e c4 02 00 04 d2 4f 29 4d 4e 75 0d 00 53 69",
                                             "6d 70 6c 65 53 65 72 76 65 72 4f 29 4f 4e 91 08 4f 29 70 4e 91 00 4f 29 79 4e 75 1c 00 42 41 43",
                                             "6e 65 74 20 53 74 61 63 6b 20 61 74 20 53 6f 75 72 63 65 46 6f 72 67 65 4f 29 78 4e 22 01 04 4f",
                                             "29 46 4e 74 00 47 4e 55 4f 29 2c 4e 75 06 00 30 2e 36 2e 30 4f 29 0c 4e 74 00 31 2e 30 4f 29 62",
                                             "4e 21 01 4f 29 8b 4e 21 0a 4f 29 61 4e 85 06 00 87 0b c8 28 f9 4f 29 60 4e 85 08 05 b4 a7 0c 08",
                                             "00 00 00 4f 29 4c 4e c4 02 00 04 d2 c4 00 00 00 00 c4 00 00 00 01 c4 00 00 00 02 c4 00 00 00 03",
                                             "c4 00 80 00 00 c4 00 80 00 01 c4 00 80 00 02 c4 00 80 00 03 c4 00 c0 00 00 c4 00 c0 00 01 c4 00",
                                             "c0 00 02 c4 00 c0 00 03 c4 00 c0 00 04 c4 01 40 00 00 c4 01 40 00 01 c4 01 40 00 02 c4 01 40 00",
                                             "03 c4 01 40 00 04 c4 01 40 00 05 c4 01 40 00 06 c4 01 40 00 07 c4 01 40 00 08 c4 01 40 00 09 c4",
                                             "03 c0 00 00 c4 03 c0 00 01 c4 05 40 00 00 c4 05 40 00 01 c4 05 40 00 02 c4 05 40 00 03 c4 05 40",
                                             "00 04 c4 05 40 00 05 c4 05 40 00 06 c4 07 00 00 00 c4 07 00 00 01 c4 07 00 00 02 c4 07 00 00 03",
                                             "c4 03 80 00 00 c4 03 80 00 01 c4 03 80 00 02 c4 03 80 00 03 c4 03 40 00 00 c4 05 00 00 00 c4 05",
                                             "00 00 01 c4 05 00 00 02 c4 05 00 00 03 c4 05 00 00 04 c4 05 00 00 05 c4 05 00 00 06 c4 05 00 00",
                                             "07 c4 02 80 00 00 c4 02 80 00 01 c4 02 80 00 02 4f 29 3e 4e 22 05 c4 4f 29 6b 4e 91 03 4f 29 0b",
                                             "4e 22 0b b8 4f 29 49 4e 21 03 4f 29 1e 4e 4f 29 9b 4e 21 01 4f 29 1c 4e 75 07 00 73 65 72 76 65",
                                             "72 4f 29 39 4e b4 14 14 0e 08 4f 29 77 4e 31 00 4f 29 38 4e a4 74 0c 0a 06 4f 29 18 4e 10 4f 29",
                                             "3a 4e 74 00 55 53 41 4f 29 98 4e 4f 1f", });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        System.out.println(String.format("bvll: %s",
                                         bvll.getClass()
                                             .getName()));
        OriginalUnicast msg = assertCast(OriginalUnicast.class, bvll);

        NetworkPDU     npdu = msg.decodePayload();
        ApplicationPDU pdu  = npdu.decodeApplicationHeader();
        System.out.println(String.format("pdu: %s",
                                         pdu.getClass()
                                            .getName()));
        Object res = pdu.decodePayload();
        System.out.println(String.format("res: %s",
                                         res.getClass()
                                            .getName()));

        ReadPropertyMultiple.Ack ack = assertCast(ReadPropertyMultiple.Ack.class, res);
        assertEquals(1, ack.list_of_read_access_results.size());

        ReadAccessResult result = ack.list_of_read_access_results.get(0);
        device           obj    = result.toObject(device.class);
        assertEquals("server", obj.description);
        assertEquals("0.6.0", obj.firmware_revision);

        if (m_verbose)
        {
            for (BACnetObjectIdentifier objId : obj.object_list)
            {
                System.out.println(String.format("obj list: %s", objId));
            }

            for (Values val : obj.protocol_services_supported.values())
            {
                System.out.println(String.format("protocol_services_supported : %s", val));
            }

            for (BACnetObjectType val : obj.protocol_object_types_supported.values())
            {
                System.out.println(String.format("protocol_object_types_supported : %s", val));
            }
        }
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_ReadPropertyMultipleResponse_b()
    {
        byte[] buf = parseHex(new String[] { "81 0A 01 25 01 00 30 D8 0E 0C 01 6E 43 C6 1E 29",
                                             "1C 4E 75 41 04 00 48 00 79 00 64 00 72 00 6F 00",
                                             "6E 00 69 00 63 00 20 00 55 00 6E 00 69 00 74 00",
                                             "20 00 48 00 65 00 61 00 74 00 65 00 72 00 20 00",
                                             "42 00 30 00 31 00 20 00 45 00 6E 00 61 00 62 00",
                                             "6C 00 65 00 64 4F 29 51 4E 10 4F 29 11 5E 91 02",
                                             "91 20 5F 29 57 4E 00 00 00 00 00 00 00 00 00 00",
                                             "00 00 00 10 00 00 4F 29 55 4E 91 00 4F 29 4B 4E",
                                             "C4 01 6E 43 C6 4F 29 0F 5E 91 02 91 20 5F 29 4F",
                                             "4E 91 05 4F 29 10 5E 91 02 91 20 5F 29 4D 4E 75",
                                             "6B 04 00 4E 00 6F 00 72 00 74 00 68 00 43 00 72",
                                             "00 65 00 65 00 6B 00 42 00 2F 00 46 00 69 00 65",
                                             "00 6C 00 64 00 20 00 42 00 75 00 73 00 31 00 2E",
                                             "00 42 00 2D 00 50 00 65 00 6E 00 74 00 31 00 2D",
                                             "00 4D 00 69 00 73 00 63 00 20 00 61 00 64 00 64",
                                             "00 20 00 34 00 38 00 2E 00 48 00 55 00 48 00 2D",
                                             "00 42 00 30 00 31 00 2D 00 45 00 4E 4F 29 43 5E",
                                             "91 02 91 20 5F 29 04 4E 75 09 04 00 54 00 72 00",
                                             "75 00 65 4F 1F", });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        System.out.println(String.format("bvll: %s",
                                         bvll.getClass()
                                             .getName()));
        OriginalUnicast msg = assertCast(OriginalUnicast.class, bvll);

        NetworkPDU     npdu = msg.decodePayload();
        ApplicationPDU pdu  = npdu.decodeApplicationHeader();
        System.out.println(String.format("pdu: %s",
                                         pdu.getClass()
                                            .getName()));
        Object res = pdu.decodePayload();
        System.out.println(String.format("res: %s",
                                         res.getClass()
                                            .getName()));

        ReadPropertyMultiple.Ack ack = assertCast(ReadPropertyMultiple.Ack.class, res);
        assertEquals(1, ack.list_of_read_access_results.size());

        ReadAccessResult        result    = ack.list_of_read_access_results.get(0);
        ReadAccessResult.Values subResult = result.list_of_results.get(3);
        BACnetPriorityArray     obj       = assertCast(BACnetPriorityArray.class, subResult.property_value);
        assertNotNull(obj.values[13]);
        assertCast(Boolean.class, obj.values[13].anyValue);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_ReadPropertyMultipleResponse_c()
    {
        byte[] buf = parseHex(new String[] { "81 0A 03 94 01 00 30 91 0E 0C 04 6E 1A 81 1E 29",
                                             "55 4E 21 02 4F 29 20 4E A4 FF FF FF FF A4 FF FF",
                                             "FF FF 4F 29 7B 4E 0E B4 00 00 00 00 21 02 B4 05",
                                             "00 00 00 21 01 B4 0E 00 00 00 21 02 0F 0E B4 00",
                                             "00 00 00 21 02 B4 05 00 00 00 21 01 B4 0E 00 00",
                                             "00 21 02 0F 0E B4 00 00 00 00 21 02 B4 05 00 00",
                                             "00 21 01 B4 0E 00 00 00 21 02 0F 0E B4 00 00 00",
                                             "00 21 02 B4 05 00 00 00 21 01 B4 0E 00 00 00 21",
                                             "02 0F 0E B4 00 00 00 00 21 02 B4 05 00 00 00 21",
                                             "01 B4 0E 00 00 00 21 02 0F 0E B4 00 00 00 00 21",
                                             "02 0F 0E B4 00 00 00 00 21 02 0F 4F 2A 0A 0D 4E",
                                             "5E 75 77 04 00 4A 00 43 00 49 00 41 00 44 00 53",
                                             "00 3A 00 4E 00 6F 00 72 00 74 00 68 00 43 00 72",
                                             "00 65 00 65 00 6B 00 43 00 2F 00 53 00 63 00 68",
                                             "00 65 00 64 00 75 00 6C 00 65 00 2E 00 4E 00 6F",
                                             "00 72 00 74 00 68 00 63 00 72 00 65 00 65 00 6B",
                                             "00 20 00 43 00 20 00 2D 00 20 00 48 00 6F 00 6C",
                                             "00 69 00 64 00 61 00 79 00 20 00 43 00 61 00 6C",
                                             "00 65 00 6E 00 64 00 61 00 72 6E 7E B4 00 00 00",
                                             "00 21 02 7F 6F 21 09 5F 4F 29 26 4E 1C 01 AE 27",
                                             "89 2E B4 00 00 00 00 21 02 2F 39 09 4F 2A 0A 0E",
                                             "4E 5E 75 93 04 00 4A 00 43 00 49 00 41 00 44 00",
                                             "53 00 3A 00 4E 00 6F 00 72 00 74 00 68 00 43 00",
                                             "72 00 65 00 65 00 6B 00 43 00 2F 00 50 00 72 00",
                                             "6F 00 67 00 72 00 61 00 6D 00 6D 00 69 00 6E 00",
                                             "67 00 2E 00 43 00 20 00 42 00 75 00 69 00 6C 00",
                                             "64 00 69 00 6E 00 67 00 2E 00 41 00 48 00 55 00",
                                             "2D 00 43 00 30 00 33 00 2E 00 41 00 48 00 55 00",
                                             "2D 00 43 00 30 00 33 00 20 00 4D 00 61 00 73 00",
                                             "74 00 65 00 72 00 20 00 4F 00 63 00 63 00 75 00",
                                             "70 00 69 00 65 00 64 6E 6F 91 55 C4 04 ED E4 F1",
                                             "21 06 65 08 0A AE 50 16 BA C0 03 E9 C4 02 00 02",
                                             "1B 5F 4F 29 36 4E 0C 04 ED E4 F1 19 55 4F 29 58",
                                             "4E 21 0F 4F 2A 03 C5 4E 22 FF FF 4F 29 AE 4E 00",
                                             "4F 29 6F 4E 82 04 00 4F 29 67 4E 91 00 4F 29 51",
                                             "4E 10 4F 2A 10 66 4E 5E B4 00 00 00 00 91 01 5F",
                                             "4F 2A 10 FD 4E 10 4F 2A 0E DF 4E 11 4F 2A 7F 0F",
                                             "4E 75 6B 04 00 4A 00 43 00 49 00 41 00 44 00 53",
                                             "00 3A 00 4E 00 6F 00 72 00 74 00 68 00 43 00 72",
                                             "00 65 00 65 00 6B 00 43 00 2F 00 53 00 63 00 68",
                                             "00 65 00 64 00 75 00 6C 00 65 00 2E 00 43 00 20",
                                             "00 42 00 6C 00 64 00 67 00 20 00 2D 00 20 00 4B",
                                             "00 69 00 74 00 63 00 68 00 65 00 6E 00 20 00 53",
                                             "00 63 00 68 00 65 00 64 00 75 00 6C 00 65 4F 2A",
                                             "09 56 4E 75 33 04 00 43 00 20 00 42 00 6C 00 64",
                                             "00 67 00 20 00 2D 00 20 00 4B 00 69 00 74 00 63",
                                             "00 68 00 65 00 6E 00 20 00 53 00 63 00 68 00 65",
                                             "00 64 00 75 00 6C 00 65 4F 29 4D 4E 75 5D 04 00",
                                             "4E 00 6F 00 72 00 74 00 68 00 43 00 72 00 65 00",
                                             "65 00 6B 00 43 00 2F 00 53 00 63 00 68 00 65 00",
                                             "64 00 75 00 6C 00 65 00 2E 00 43 00 20 00 42 00",
                                             "6C 00 64 00 67 00 20 00 2D 00 20 00 4B 00 69 00",
                                             "74 00 63 00 68 00 65 00 6E 00 20 00 53 00 63 00",
                                             "68 00 65 00 64 00 75 00 6C 00 65 4F 29 1C 4E 71",
                                             "04 4F 2A 02 00 4E 91 00 4F 2A 02 A1 4E 11 4F 2A",
                                             "08 95 4E 91 00 4F 2A 03 8C 4E 91 05 4F 2A 03 EE",
                                             "4E 91 00 4F 29 4F 4E 91 11 4F 29 4B 4E C4 04 6E",
                                             "1A 81 4F 1F", });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        System.out.println(String.format("bvll: %s",
                                         bvll.getClass()
                                             .getName()));
        OriginalUnicast msg = assertCast(OriginalUnicast.class, bvll);

        NetworkPDU     npdu = msg.decodePayload();
        ApplicationPDU pdu  = npdu.decodeApplicationHeader();
        System.out.println(String.format("pdu: %s",
                                         pdu.getClass()
                                            .getName()));
        Object res = pdu.decodePayload();
        System.out.println(String.format("res: %s",
                                         res.getClass()
                                            .getName()));

        ReadPropertyMultiple.Ack ack = assertCast(ReadPropertyMultiple.Ack.class, res);
        assertEquals(1, ack.list_of_read_access_results.size());

        ReadAccessResult               result    = ack.list_of_read_access_results.get(0);
        ReadAccessResult.Values        subResult = result.list_of_results.get(3);
        TagContextCommon.CustomWrapper obj       = assertCast(TagContextCommon.CustomWrapper.class, subResult.property_value);
        assertNotNull(obj);
        assertEquals(1, obj.fields.size());
        assertEquals(5, obj.fields.get(0).contextTag);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_ReadPropertyMultipleResponse_d()
    {
        byte[] buf = parseHex(new String[] { "81 0A 00 E6 01 08 00 46 01 04 30 AE 0E 0C 03 C0",
                                             "00 01 1E 29 4B 4E C4 03 C0 00 01 4F 29 4D 4E 75",
                                             "11 00 57 61 72 6E 69 6E 67 20 4E 6F 74 69 66 69",
                                             "65 72 4F 29 4F 4E 91 0F 4F 29 11 4E 21 01 4F 29",
                                             "56 4E 21 FF 21 FF 21 FF 4F 29 01 4E 82 05 E0 4F",
                                             "29 66 4E 82 01 FE B4 00 00 00 00 B4 17 3B 3B 63",
                                             "1E 22 FF FF 60 1F 21 00 10 82 05 E0 82 01 FE B4",
                                             "00 00 00 00 B4 17 3B 3B 63 1E 22 FF FF 60 1F 21",
                                             "00 10 82 05 E0 82 01 FE B4 00 00 00 00 B4 17 3B",
                                             "3B 63 1E 22 FF FF 60 1F 21 00 10 82 05 E0 82 01",
                                             "FE B4 00 00 00 00 B4 17 3B 3B 63 1E 22 FF FF 60",
                                             "1F 21 00 10 82 05 E0 82 01 FE B4 00 00 00 00 B4",
                                             "17 3B 3B 63 1E 22 FF FF 60 1F 21 00 10 82 05 E0",
                                             "4F 29 1C 4E 75 0E 00 53 65 6E 64 73 20 77 61 72",
                                             "6E 69 6E 67 4F 1F", });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        System.out.println(String.format("bvll: %s",
                                         bvll.getClass()
                                             .getName()));
        OriginalUnicast msg = assertCast(OriginalUnicast.class, bvll);

        NetworkPDU     npdu = msg.decodePayload();
        ApplicationPDU pdu  = npdu.decodeApplicationHeader();
        System.out.println(String.format("pdu: %s",
                                         pdu.getClass()
                                            .getName()));
        Object res = pdu.decodePayload();
        System.out.println(String.format("res: %s",
                                         res.getClass()
                                            .getName()));

        ReadPropertyMultiple.Ack ack = assertCast(ReadPropertyMultiple.Ack.class, res);
        assertEquals(1, ack.list_of_read_access_results.size());

        ReadAccessResult        result    = ack.list_of_read_access_results.get(0);
        ReadAccessResult.Values subResult = result.list_of_results.get(6);

        @SuppressWarnings("unchecked") List<BACnetDestination> lst = assertCast(ArrayList.class, subResult.property_value);
        assertNotNull(lst);
        assertEquals(5, lst.size());
        BACnetDestination dst = lst.get(3);
        assertEquals("GLOBAL", dst.recipient.address.toString());
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_ReadPropertyMultipleResponse_e()
    {
        byte[] buf = parseHex(new String[] { "81 0A 00 D9 01 00 30 EF 0E 0C 02 40 00 07 1E 29",
                                             "4B 4E C4 02 40 00 07 4F 29 4D 4E 75 06 00 45 45",
                                             "4F 20 37 4F 29 4F 4E 91 09 4F 29 1C 4E 75 2A 00",
                                             "43 34 32 35 20 45 6C 65 76 20 4D 61 63 68 20 52",
                                             "6F 6F 6D 20 31 39 30 32 20 53 70 61 63 65 20 54",
                                             "65 6D 70 20 41 6C 61 72 6D 4F 29 25 4E 91 05 4F",
                                             "29 48 4E 91 00 4F 29 53 4E 5E 09 3C 1C 00 00 00",
                                             "00 2C 42 AA 00 00 3C 00 00 00 00 5F 4F 29 4E 4E",
                                             "0C 00 00 03 E8 19 55 4F 29 24 4E 91 00 4F 29 23",
                                             "4E 82 05 E0 4F 29 00 4E 82 05 00 4F 29 11 4E 21",
                                             "02 4F 29 82 4E 2E A4 75 08 0B 05 B4 09 11 2D 00",
                                             "2F 2E A4 75 06 18 06 B4 0B 0D 0E 00 2F 2E A4 75",
                                             "08 0B 05 B4 12 00 14 00 2F 4F 2A 04 67 4E 21 0A",
                                             "4F 2A 04 68 4E 21 00 4F 1F", });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        OriginalUnicast      msg  = assertCast(OriginalUnicast.class, bvll);

        NetworkPDU npdu = msg.decodePayload();

        ApplicationPDU pdu = npdu.decodeApplicationHeader();
        Object         res = pdu.decodePayload();

        ReadPropertyMultiple.Ack ack = assertCast(ReadPropertyMultiple.Ack.class, res);
        assertEquals(1, ack.list_of_read_access_results.size());

        ReadAccessResult        result    = ack.list_of_read_access_results.get(0);
        ReadAccessResult.Values subResult = result.list_of_results.get(6);
        BACnetEventParameter    val       = assertCast(BACnetEventParameter.class, subResult.property_value);
        assertNotNull(val.out_of_range);
        assertEquals(Unsigned32.box(60), val.out_of_range.time_delay);
        assertEquals(85.0, val.out_of_range.high_limit, 0.1);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_UnconfirmedEventNotification_a()
    {
        byte[] buf = parseHex(new String[] { "81 04 00 CB A5 97 3F FA BA C0 01 2A FF FF 00 9C",
                                             "AA 06 A5 97 65 FA BA C1 FE 10 03 09 00 1C 02 0F",
                                             "B8 38 2C 4A 40 00 01 3E 2E A4 76 06 13 02 B4 0E",
                                             "1A 27 58 2F 3F 49 07 59 78 69 02 7D 78 00 56 61",
                                             "6C 69 64 20 41 63 63 65 73 73 20 28 31 29 3A 20",
                                             "31 30 33 30 32 30 30 2E 44 43 32 30 31 30 30 31",
                                             "2C 43 55 35 31 32 31 2C 32 34 34 37 30 36 32 28",
                                             "33 37 29 2C 30 2C 0A 34 2D 37 2D 31 20 4E 57 52",
                                             "4F 20 4E 6F 72 74 68 20 43 6F 72 72 69 64 6F 72",
                                             "20 44 6F 6F 72 20 23 31 20 44 6F 6F 72 20 43 6F",
                                             "6E 74 72 6F 6C 6C 65 72 0A 4A 45 41 4E 4E 45 20",
                                             "54 52 41 4E 0A 89 01 99 00 A9 00 B9 00 CE 2E 0E",
                                             "1C 49 EE 5E 70 0F 1A 04 00 2F CF", });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        Forwarded            msg  = assertCast(Forwarded.class, bvll);

        NetworkPDU npdu = msg.decodePayload();

        ApplicationPDU pdu = npdu.decodeApplicationHeader();
        Object         res = pdu.decodePayload();

        UnconfirmedEventNotification event = assertCast(UnconfirmedEventNotification.class, res);
        assertEquals(1030200, event.initiating_device_identifier.instance_number.unbox());
        assertEquals(120, event.priority.unbox());
        assertTrue(event.event_values.isPresent());

        BACnetNotificationParameters.typefor_change_of_value change_of_value = event.event_values.get().change_of_value;
        assertEquals(1952718, change_of_value.new_value.changed_value, 0.1);
        assertFalse(change_of_value.status_flags.isSet(BACnetStatusFlags.Values.in_alarm));
        assertFalse(change_of_value.status_flags.isSet(BACnetStatusFlags.Values.fault));
        assertFalse(change_of_value.status_flags.isSet(BACnetStatusFlags.Values.overridden));
        assertFalse(change_of_value.status_flags.isSet(BACnetStatusFlags.Values.out_of_service));
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_UnconfirmedEventNotification_b()
    {
        byte[] buf = parseHex(new String[] { "81 04 00 86 A5 97 3F FA BA C0 01 2B FF FF 00 9C",
                                             "AA 06 A5 97 65 FA BA C1 FE 10 03 09 00 1C 02 0F",
                                             "B8 38 2C 47 C3 24 B1 3E 2E A4 76 06 14 03 B4 08",
                                             "2A 24 17 2F 3F 49 08 59 1E 69 06 7D 3D 00 34 2D",
                                             "31 32 2D 31 20 4E 57 52 4F 20 4C 6F 62 62 79 20",
                                             "44 6F 6F 72 20 23 31 20 43 6F 6E 28 31 30 33 30",
                                             "32 30 30 2E 44 43 32 30 36 30 30 31 29 0D 0A 46",
                                             "6F 72 63 65 64 20 4F 70 65 6E 89 00 99 01 A9 00",
                                             "B9 02 CE 6E 6F CF", });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        Forwarded            msg  = assertCast(Forwarded.class, bvll);

        NetworkPDU npdu = msg.decodePayload();

        ApplicationPDU pdu = npdu.decodeApplicationHeader();
        Object         res = pdu.decodePayload();

        UnconfirmedEventNotification event = assertCast(UnconfirmedEventNotification.class, res);
        assertEquals(1030200, event.initiating_device_identifier.instance_number.unbox());
        assertEquals(30, event.priority.unbox());
        assertTrue(event.event_values.isPresent());
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_ReadPropertyMultipleWithCustomProperties()
    {
        byte[] buf = parseHex(new String[] { "81 0a 00 f5 01 00 30 21 0e 0c 00 00 00 01 1e 29 4b 4e c4 00 00 00 01 4f 29 4d 4e 75 0f 00 41 4e",
                                             "41 4c 4f 47 20 49 4e 50 55 54 20 31 4f 29 4f 4e 91 00 4f 29 55 4e 44 00 00 00 00 4f 29 6f 4e 82",
                                             "04 00 4f 29 24 4e 91 00 4f 29 51 4e 10 4f 29 75 4e 91 62 4f 29 1c 4e 75 0f 00 41 4e 41 4c 4f 47",
                                             "20 49 4e 50 55 54 20 31 4f 29 67 4e 91 00 4f 29 71 4e 21 00 4f 29 11 4e 23 3f ff ff 4f 29 2d 4e",
                                             "44 00 00 00 00 4f 29 3b 4e 44 00 00 00 00 4f 29 19 4e 44 00 00 00 00 4f 29 34 4e 82 06 00 4f 29",
                                             "23 4e 82 05 00 4f 29 00 4e 82 05 e0 4f 29 48 4e 91 00 4f 29 82 4e 2e a4 ff ff ff ff b4 ff ff ff",
                                             "ff 2f 2e a4 ff ff ff ff b4 ff ff ff ff 2f 2e a4 ff ff ff ff b4 ff ff ff ff 2f 4f 2a 27 0d 4e 44",
                                             "42 b5 05 1f 4f 2a 27 0e 4e 21 5a 4f 2a 27 0f 4e 32 ff 38 4f 1f" });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        System.out.println(String.format("bvll: %s",
                                         bvll.getClass()
                                             .getName()));
        if (bvll instanceof NetworkPayload)
        {
            NetworkPayload msg = (NetworkPayload) bvll;

            NetworkPDU     npdu = msg.decodePayload();
            ApplicationPDU pdu  = npdu.decodeApplicationHeader();
            System.out.println(String.format("pdu: %s",
                                             pdu.getClass()
                                                .getName()));
            Object res = pdu.decodePayload();
            System.out.println(String.format("res: %s",
                                             res.getClass()
                                                .getName()));

            ReadPropertyMultiple.Ack ack = assertCast(ReadPropertyMultiple.Ack.class, res);
            assertEquals(1, ack.list_of_read_access_results.size());

            ReadAccessResult results = ack.list_of_read_access_results.get(0);

            assertEquals(9997, results.list_of_results.get(20).property_identifier.unknown);
            assertEquals(9998, results.list_of_results.get(21).property_identifier.unknown);
            assertEquals(9999, results.list_of_results.get(22).property_identifier.unknown);
        }
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_EpicsRun() throws
                                   IOException
    {
        final String c_txPrefix = "TX: 00000000: ";
        final String c_rxPrefix = "RX: 00000000: ";

        List<String> lines   = loadResourceAsLines(c_epics_run, false);
        OutputBuffer ob      = null;
        boolean      rxOn    = false;
        boolean      txOn    = false;
        int          lineNum = 1;

        for (String line : lines)
        {
            if (line.startsWith(c_txPrefix))
            {
                if (ob != null)
                {
                    processPart(ob);
                }

                ob   = new OutputBuffer();
                rxOn = false;
                txOn = true;
            }

            if (line.startsWith(c_rxPrefix))
            {
                if (ob != null)
                {
                    processPart(ob);
                }

                ob   = new OutputBuffer();
                txOn = false;
                rxOn = true;
            }

            if (txOn)
            {
                if (line.startsWith("TX: "))
                {
                    parseHex(ob, line.substring(c_txPrefix.length()));
                }
                else
                {
                    processPart(ob);
                    ob   = null;
                    txOn = false;
                }
            }

            if (rxOn)
            {
                if (line.startsWith("RX: "))
                {
                    parseHex(ob, line.substring(c_rxPrefix.length()));
                }
                else
                {
                    processPart(ob);
                    ob   = null;
                    rxOn = false;
                }
            }

            if (m_verbose)
            {
                System.out.println(String.format("@@@ INPUT: %d: %s", lineNum++, line));
            }
        }
    }

    private void processPart(OutputBuffer ob)
    {
        if (m_verbose)
        {
            System.out.println(String.format("####################################################################### %d", ob.size()));
        }

        try
        {
            InputBuffer ib = new InputBuffer(ob);

            BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
            if (m_verbose)
            {
                System.out.println(String.format("bvll: %s",
                                                 bvll.getClass()
                                                     .getName()));
            }

            if (bvll instanceof NetworkPayload)
            {
                NetworkPayload msg = (NetworkPayload) bvll;

                NetworkPDU npdu = msg.decodePayload();

                ApplicationPDU pdu = npdu.decodeApplicationHeader();
                if (m_verbose)
                {
                    System.out.println(String.format("pdu: %s",
                                                     pdu.getClass()
                                                        .getName()));
                }

                Object res = pdu.decodePayload();
                if (m_verbose)
                {
                    System.out.println(String.format("res: %s",
                                                     res.getClass()
                                                        .getName()));
                }

                if (res instanceof ReadPropertyMultiple.Ack)
                {
                    ReadPropertyMultiple.Ack ack = (ReadPropertyMultiple.Ack) res;

                    for (ReadAccessResult result : ack.list_of_read_access_results)
                    {
                        BACnetObjectModel obj = result.toObject(BACnetObjectModel.class);

                        String            json = obj.serializeToJson();
                        BACnetObjectModel obj2 = BACnetObjectModel.deserializeFromJson(BACnetObjectModel.class, json);

                        if (m_verbose)
                        {
                            System.out.println(String.format("obj: %s",
                                                             obj.getClass()
                                                                .getName()));
                        }
                    }
                }

                OutputBuffer output = new OutputBuffer();
                pdu.encodeHeader(output);
                output.emitNestedBlock(ApplicationPDU.encodePayload(res));

                byte[] targetBuf = output.toByteArray();
                byte[] sourceBuf = npdu.getPayload();

                if (comparePayloads(targetBuf, sourceBuf, targetBuf.length))
                {
                    if (m_verbose)
                    {
                        System.out.println("PERFECT MATCH!");
                    }
                }
            }
        }
        catch (Exception e)
        {
            //e.printStackTrace(System.out);
            throw new RuntimeException(e);
        }
    }

    private boolean comparePayloads(byte[] targetBuf,
                                    byte[] sourceBuf,
                                    int length)
    {
        for (int i = 0; i < length; i++)
        {
            if (i >= sourceBuf.length)
            {
                dumpContext("Src", sourceBuf);
                dumpContext("Dst", targetBuf);
                String txt = String.format("Values at offset %d differ: Expected end of stream Got=%d", i, targetBuf[i]);
                failOrPrint(txt);
                return false;
            }

            if (targetBuf[i] != sourceBuf[i])
            {
                dumpContext("Src", sourceBuf);
                dumpContext("Dst", targetBuf);
                String txt = String.format("Values at offset %d differ: Expected=%02x Got=%02x", i, sourceBuf[i], targetBuf[i]);
                failOrPrint(txt);
                return false;
            }
        }
        return true;
    }

    private void failOrPrint(String txt)
    {
        fail(txt);
        //        System.out.println(txt);
    }

    private void dumpContext(String prefix,
                             byte[] buf)
    {
        if (!m_verbose)
        {
            return;
        }

        BufferUtils.convertToHex(buf, 0, buf.length, 32, true, System.out::println);
    }

    //--//

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void network_any()
    {
        byte[] buf = parseHex(new String[] { "81 0a 00 f5 01 00 30 21 0e 0c 00 00 00 01 1e 29 4b 4e c4 00 00 00 01 4f 29 4d 4e 75 0f 00 41 4e",
                                             "41 4c 4f 47 20 49 4e 50 55 54 20 31 4f 29 4f 4e 91 00 4f 29 55 4e 44 00 00 00 00 4f 29 6f 4e 82",
                                             "04 00 4f 29 24 4e 91 00 4f 29 51 4e 10 4f 29 75 4e 91 62 4f 29 1c 4e 75 0f 00 41 4e 41 4c 4f 47",
                                             "20 49 4e 50 55 54 20 31 4f 29 67 4e 91 00 4f 29 71 4e 21 00 4f 29 11 4e 23 3f ff ff 4f 29 2d 4e",
                                             "44 00 00 00 00 4f 29 3b 4e 44 00 00 00 00 4f 29 19 4e 44 00 00 00 00 4f 29 34 4e 82 06 00 4f 29",
                                             "23 4e 82 05 00 4f 29 00 4e 82 05 e0 4f 29 48 4e 91 00 4f 29 82 4e 2e a4 ff ff ff ff b4 ff ff ff",
                                             "ff 2f 2e a4 ff ff ff ff b4 ff ff ff ff 2f 2e a4 ff ff ff ff b4 ff ff ff ff 2f 4f 2a 27 0d 4e 44",
                                             "42 b5 05 1f 4f 2a 27 0e 4e 21 5a 4f 2a 27 0f 4e 32 ff 38 4f 1f" });

        InputBuffer ib = InputBuffer.createFrom(buf);

        BaseVirtualLinkLayer bvll = BaseVirtualLinkLayer.decode(ib);
        if (m_verbose)
        {
            System.out.println(String.format("bvll: %s",
                                             bvll.getClass()
                                                 .getName()));
        }

        if (bvll instanceof NetworkPayload)
        {
            NetworkPayload msg = (NetworkPayload) bvll;

            NetworkPDU     npdu = msg.decodePayload();
            ApplicationPDU pdu  = npdu.decodeApplicationHeader();
            if (m_verbose)
            {
                System.out.println(String.format("pdu: %s",
                                                 pdu.getClass()
                                                    .getName()));
            }

            Object res = pdu.decodePayload();
            if (m_verbose)
            {
                System.out.println(String.format("res: %s",
                                                 res.getClass()
                                                    .getName()));
            }
        }
    }

    //--//

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void iAm()
    {
        byte[] buf = parse(new String[] { "X'10' PDU Type=1 (Unconfirmed-Service-Request-PDU) ",
                                          "X'00' Service Choice=0 (I-Am-Request) ",
                                          "X'C4' Application Tag 12 (Object Identifier, L=4) (I-Am Device Identifier) ",
                                          "  X'02000003' Device, Instance Number=3 ",
                                          "X'22' Application Tag 2 (Unsigned Integer, L=2) (Max APDU Length Accepted) ",
                                          "  X'0400' 1024 ",
                                          "X'91' Application Tag 9 (Enumerated, L=1) (Segmentation Supported) ",
                                          "  X'03' 3 (NO_SEGMENTATION) ",
                                          "X'21' Application Tag 2 (Unsigned Integer, L=1) (Vendor ID) ",
                                          "  X'63' 99 ", });

        InputBuffer    ib  = InputBuffer.createFrom(buf);
        ApplicationPDU pdu = ApplicationPDU.decodeHeader(ib);

        UnconfirmedRequestPDU pdu2 = assertCast(UnconfirmedRequestPDU.class, pdu);
        assertEquals(UnconfirmedServiceChoice.i_am, pdu2.serviceChoice);

        Object res  = pdu2.decodePayload();
        IAm    res2 = assertCast(IAm.class, res);
        assertEquals(BACnetObjectType.device, res2.i_am_device_identifier.object_type.value);
        assertUnsignedEquals(3, res2.i_am_device_identifier.instance_number);

        assertUnsignedEquals(1024, res2.max_apdu_length_accepted);
        assertUnsignedEquals(99, res2.vendor_id);

        testSweep(buf, 3);

        //--//

        OutputBuffer ob = new OutputBuffer();
        pdu2.encodeHeader(ob);
        ob.emitNestedBlock(ApplicationPDU.encodePayload(res2));

        byte[] clone_buf = ob.toByteArray();
        dumpByteArray(clone_buf);

        assertArrayEquals(buf, clone_buf);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void confirmedCOVNotification()
    {
        byte[] buf = parse(new String[] { "X'00' PDU Type=0 (BACnet-Confirmed-Request-PDU, SEG=0, MOR=0, SA=0)",
                                          "X'02' Maximum APDU Size Accepted=206 octets",
                                          "X'0F' Invoke ID=15",
                                          "X'01' Service Choice=1 (ConfirmedCOVNotification-Request)",
                                          "X'09' SD Context Tag 0 (Subscriber Process Identifier, L=1)",
                                          "X'12' Subscriber Process Identifier=18",
                                          "X'1C' SD Context Tag 1 (Initiating Device Identifier, L=4)",
                                          "X'02000004' Device, Instance 4",
                                          "X'2C' SD Context Tag 2 (Monitored Object Identifier, L=4)",
                                          "X'0000000A' Analog Input, Instance Number=10",
                                          "X'39' SD Context Tag 3 (Time Remaining, L=1)",
                                          "X'00' Time Remaining=0",
                                          "X'4E' PD Opening Tag 4 (List of Values)",
                                          "    X'09' SD Context Tag 0 (Property Identifier, L=1)",
                                          "    X'55' 85 (PRESENT_VALUE)",
                                          "    X'2E' PD Opening Tag 2 (Value)",
                                          "        X'44' Application Tag 4 (Real, L=4)",
                                          "        X'42820000' 65.0",
                                          "    X'2F' PD Closing Tag 2 (Value)",
                                          "    X'09' SD Context Tag 0 (Property Identifier, L=1)",
                                          "    X'6F' 111 (STATUS_FLAGS)",
                                          "    X'2E' PD Opening Tag 2 (Value)",
                                          "        X'82' Application Tag 8 (Bit String, L=2)",
                                          "        X'0400' 0,0,0,0 (FALSE, FALSE, FALSE, FALSE)",
                                          "    X'2F' PD Closing Tag 2 (Value)",
                                          "X'4F' PD Closing Tag 4 (List Of Values)" });

        InputBuffer    ib  = InputBuffer.createFrom(buf);
        ApplicationPDU pdu = ApplicationPDU.decodeHeader(ib);

        ConfirmedRequestPDU pdu2 = assertCast(ConfirmedRequestPDU.class, pdu);
        assertEquals(206, pdu2.getMaxApduLengthAccepted());
        assertUnsignedEquals(15, pdu2.invokeId);
        assertEquals(ConfirmedServiceChoice.confirmed_cov_notification, pdu2.serviceChoice);

        Object                   res  = pdu2.decodePayload();
        ConfirmedCOVNotification res2 = assertCast(ConfirmedCOVNotification.class, res);
        assertEquals(BACnetObjectType.device, res2.initiating_device_identifier.object_type.value);
        assertUnsignedEquals(4, res2.initiating_device_identifier.instance_number);

        assertEquals(BACnetObjectType.analog_input, res2.monitored_object_identifier.object_type.value);
        assertUnsignedEquals(10, res2.monitored_object_identifier.instance_number);

        assertEquals(2, res2.list_of_values.size());
        BACnetPropertyValue val1 = res2.list_of_values.get(0);
        assertEquals(BACnetPropertyIdentifier.present_value, val1.property_identifier.value);
        assertEquals(65.0, (float) val1.property_value, 0.1);
        BACnetPropertyValue val2 = res2.list_of_values.get(1);
        assertEquals(BACnetPropertyIdentifier.status_flags, val2.property_identifier.value);

        testSweep(buf, 5);

        //--//

        OutputBuffer ob = new OutputBuffer();
        pdu2.encodeHeader(ob);
        ob.emitNestedBlock(ApplicationPDU.encodePayload(res2));

        byte[] clone_buf = ob.toByteArray();
        dumpByteArray(clone_buf);

        assertArrayEquals(buf, clone_buf);

        //--//

        analog_input obj = res2.toObject(analog_input.class);
        assertEquals(65.0, obj.present_value, 0.1);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void readProperty()
    {
        byte[] buf = parse(new String[] { "X'00' PDU Type=0 (BACnet-Confirmed-Request-PDU, SEG=0, MOR=0, SA=0)",
                                          "X'00' Maximum APDU Size Accepted=50 octets",
                                          "X'01' Invoke ID=1",
                                          "X'0C' Service Choice=12 (ReadProperty-Request)",
                                          "X'0C' SD Context Tag 0 (Object Identifier, L=4)",
                                          " X'00000005' Analog Input, Instance Number=5",
                                          "X'19' SD Context Tag 1 (Property Identifier, L=1)",
                                          " X'55' 85 (PRESENT_VALUE)" });

        InputBuffer         ib   = InputBuffer.createFrom(buf);
        ApplicationPDU      pdu  = ApplicationPDU.decodeHeader(ib);
        ConfirmedRequestPDU pdu2 = assertCast(ConfirmedRequestPDU.class, pdu);
        assertEquals(ConfirmedServiceChoice.read_property, pdu2.serviceChoice);

        Object       res  = pdu2.decodePayload();
        ReadProperty res2 = assertCast(ReadProperty.class, res);
        assertEquals(BACnetObjectType.analog_input, res2.object_identifier.object_type.value);
        assertUnsignedEquals(5, res2.object_identifier.instance_number);
        assertEquals(BACnetPropertyIdentifier.present_value, res2.property_identifier.value);

        testSweep(buf, 5);

        //--//

        OutputBuffer        ob   = new OutputBuffer();
        ConfirmedRequestPDU pdu3 = res2.preparePCI();
        pdu3.invokeId = Unsigned8.box(1);
        pdu3.encodeHeader(ob);
        ob.emitNestedBlock(ApplicationPDU.encodePayload(res2));

        byte[] clone_buf = ob.toByteArray();
        dumpByteArray(clone_buf);

        assertArrayEquals(buf, clone_buf);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void readPropertyResponse()
    {
        byte[] buf = parse(new String[] { "X'30' PDU Type=3 (BACnet-ComplexACK-PDU, SEG=0, MOR=0)",
                                          "X'01' Invoke ID=1",
                                          "X'0C' Service ACK Choice=12 (ReadProperty-ACK)",
                                          "X'0C' SD Context Tag 0 (Object Identifier, L=4)",
                                          " X'00000005' Analog Input, Instance Number=5",
                                          "X'19' SD Context Tag 1 (Property Identifier, L=1)",
                                          "  X'55' 85 (PRESENT_VALUE)",
                                          "X'3E' PD Opening Tag 3 (Property Value)",
                                          "    X'44' Application Tag 4 (Real, L=4)",
                                          "     X'4290999A' 72.3",
                                          "X'3F' PD Closing Tag 3 (Property Value)" });

        InputBuffer    ib   = InputBuffer.createFrom(buf);
        ApplicationPDU pdu  = ApplicationPDU.decodeHeader(ib);
        ComplexAckPDU  pdu2 = assertCast(ComplexAckPDU.class, pdu);
        assertEquals(ConfirmedServiceChoice.read_property, pdu2.serviceChoice);

        Object res = pdu2.decodePayload();
        assertEquals(ReadProperty.Ack.class, res.getClass());
        ReadProperty.Ack res2 = (ReadProperty.Ack) res;
        assertEquals(BACnetObjectType.analog_input, res2.object_identifier.object_type.value);
        assertUnsignedEquals(5, res2.object_identifier.instance_number);
        assertEquals(BACnetPropertyIdentifier.present_value, res2.property_identifier.value);

        assertEquals(Float.class, res2.property_value.getClass());
        assertEquals(72.3, (float) res2.property_value, 0.01);

        testSweep(buf, 5);

        //--//

        OutputBuffer ob = new OutputBuffer();
        pdu2.encodeHeader(ob);
        ob.emitNestedBlock(ApplicationPDU.encodePayload(res2));

        byte[] clone_buf = ob.toByteArray();
        dumpByteArray(clone_buf);

        assertArrayEquals(buf, clone_buf);

        InputBuffer    clone_ib   = InputBuffer.createFrom(clone_buf);
        ApplicationPDU clone_pdu  = ApplicationPDU.decodeHeader(clone_ib);
        ComplexAckPDU  clone_pdu2 = assertCast(ComplexAckPDU.class, clone_pdu);
        assertEquals(ConfirmedServiceChoice.read_property, pdu2.serviceChoice);

        Object           clone_res  = clone_pdu2.decodePayload();
        ReadProperty.Ack clone_res2 = assertCast(ReadProperty.Ack.class, clone_res);
        assertEquals(BACnetObjectType.analog_input, clone_res2.object_identifier.object_type.value);

        //--//

        analog_input obj = res2.toObject(analog_input.class);
        assertEquals(72.3, obj.present_value, 0.1);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void createObject()
    {
        byte[] buf = parse(new String[] { "X'00' PDU Type=0 (BACnet-Confirmed-Request-PDU, SEG=0, MOR=0, SA=0)",
                                          "X'04' Maximum APDU Size Accepted=1024 octets",
                                          "X'56' Invoke ID=86",
                                          "X'0A' Service Choice=10 (CreateObject-Request)",
                                          "X'0E' PD Opening Tag 0 (Object Specifier)",
                                          "    X'09' SD Context Tag 0 (Object Type, L=1)",
                                          "     X'0A' 10 (File Object)",
                                          "X'0F' PD Closing Tag 0 (Object Specifier)",
                                          "X'1E' PD Opening Tag 1 (List Of Initial Values)",
                                          "    X'09' SD Context Tag 0 (Property Identifier, L=1)",
                                          "     X'29' 41 (FILE_ACCESS_METHOD)",
                                          "    X'2E' PD Opening Tag 2 (Value)",
                                          "        X'91' Application Tag 9 (Enumerated, L=1)",
                                          "         X'00' 0 (RECORD_ACCESS)",
                                          "    X'2F' PD Closing Tag 2 (Value)",
                                          "    X'09' SD Context Tag 0 (Property Identifier, L=1)",
                                          "     X'4D' 77 (OBJECT_NAME)",
                                          "    X'2E' PD Opening Tag 2 (Value)",
                                          "        X'75' Application Tag 7 (Character String, L>4)",
                                          "        X'08' Extended Length=8",
                                          "         X'00' ISO 10646 (UTF-8) Encoding",
                                          "         X'5472656E642031' \"Trend 1\"",
                                          "    X'2F' PD Closing Tag 2 (Value)",
                                          "X'1F' PD Closing Tag 1 (List Of Initial Values)" });

        InputBuffer         ib   = InputBuffer.createFrom(buf);
        ApplicationPDU      pdu  = ApplicationPDU.decodeHeader(ib);
        ConfirmedRequestPDU pdu2 = assertCast(ConfirmedRequestPDU.class, pdu);
        assertEquals(ConfirmedServiceChoice.create_object, pdu2.serviceChoice);

        Object       res  = pdu2.decodePayload();
        CreateObject res2 = assertCast(CreateObject.class, res);
        assertEquals(BACnetObjectType.file, res2.object_specifier.object_type.value);
        assertTrue(res2.list_of_initial_values.isPresent());

        List<BACnetPropertyValue> list = res2.list_of_initial_values.get();
        assertEquals(2, list.size());

        BACnetPropertyValue prop1 = list.get(0);
        assertEquals(BACnetPropertyIdentifier.file_access_method, prop1.property_identifier.value);
        BACnetFileAccessMethod prop1Val = assertCast(BACnetFileAccessMethod.class, prop1.property_value);
        assertEquals(BACnetFileAccessMethod.record_access, prop1Val);

        BACnetPropertyValue prop2 = list.get(1);
        assertEquals(BACnetPropertyIdentifier.object_name, prop2.property_identifier.value);
        assertEquals("Trend 1", prop2.property_value);

        testSweep(buf, 9);

        //--//

        OutputBuffer ob = new OutputBuffer();
        pdu2.encodeHeader(ob);
        ob.emitNestedBlock(ApplicationPDU.encodePayload(res2));

        byte[] clone_buf = ob.toByteArray();
        dumpByteArray(clone_buf);

        assertArrayEquals(buf, clone_buf);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void writeGroup()
    {
        byte[] buf = parse(new String[] { "X'10' PDU Type=1 (BACnet-Unconfirmed-Request-PDU)",
                                          "X'0A' Service Choice=10 (WriteGroup-Request)",
                                          "X'09' SD Context Tag 0 (Group Number, L=1)",
                                          " X'17' 23",
                                          "X'19' SD Context Tag 1 (Write Priority, L=1)",
                                          " X'08' 8",
                                          "X'2E' PD Opening Tag 2 (Change List)",
                                          "    X'09' SD Context Tag 0 (Channel, L=1)",
                                          "     X'0C' 12",
                                          "    X'22' Application Tag Unsigned L=2 (value)",
                                          "     X'0457' 1111",
                                          "    X'09' SD Context Tag 0 (Channel, L=1)",
                                          "     X'0D' 13",
                                          "    X'19' SD Context Tag 1 (overridingPriority, L=1)",
                                          "     X'0A' 10",
                                          "    X'74' Application Tag Charstring L=4 (value)",
                                          "     X'00' 0 (Charset UTF-8)",
                                          "     X'414243' \"ABC\"",
                                          "X'2F' PD Closing Tag 2", });

        InputBuffer           ib   = InputBuffer.createFrom(buf);
        ApplicationPDU        pdu  = ApplicationPDU.decodeHeader(ib);
        UnconfirmedRequestPDU pdu2 = assertCast(UnconfirmedRequestPDU.class, pdu);
        assertEquals(UnconfirmedServiceChoice.write_group, pdu2.serviceChoice);

        Object     res  = pdu2.decodePayload();
        WriteGroup res2 = assertCast(WriteGroup.class, res);
        assertUnsignedEquals(23, res2.group_number);

        testSweep(buf, 3);

        //--//

        OutputBuffer ob = new OutputBuffer();
        pdu2.encodeHeader(ob);
        ob.emitNestedBlock(ApplicationPDU.encodePayload(res2));

        byte[] clone_buf = ob.toByteArray();
        dumpByteArray(clone_buf);

        assertArrayEquals(buf, clone_buf);
    }

    //--//

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void serializeObject() throws
                                  IOException
    {
        analog_input obj = new analog_input();

        BACnetPropertyIdentifierOrUnknown[] propList = new BACnetPropertyIdentifierOrUnknown[] { BACnetPropertyIdentifier.object_name.forRequest(),
                                                                                                 BACnetPropertyIdentifier.present_value.forRequest() };

        obj.setValue(BACnetPropertyIdentifier.present_value, 1.3f);
        obj.setValue(BACnetPropertyIdentifier.property_list, propList);
        obj.setValue(BACnetPropertyIdentifier.time_delay, 100);

        String json = obj.serializeToJson();

        analog_input obj_res = BACnetObjectModel.deserializeFromJson(analog_input.class, json);
        assertEquals(obj.present_value, obj_res.present_value, 0);
        assertEquals(obj.time_delay, obj_res.time_delay);
        assertArrayEquals(obj.property_list, obj_res.property_list);

        //--//

        device obj2 = new device();

        List<BACnetAddressBinding> list    = Lists.newArrayList();
        BACnetAddressBinding       binding = new BACnetAddressBinding();
        binding.device_identifier = new BACnetObjectIdentifier(BACnetObjectType.analog_value, 1);
        list.add(binding);

        obj2.setValue(BACnetPropertyIdentifier.device_address_binding, list);

        String json2 = obj2.serializeToJson();

        device obj2_res = BACnetObjectModel.deserializeFromJson(device.class, json2);
        assertEquals(obj2.device_address_binding.size(), obj2_res.device_address_binding.size());
        assertEquals(obj2.device_address_binding.get(0).device_identifier, obj2_res.device_address_binding.get(0).device_identifier);
    }

    //--//

    private void checkEncodingOfNetworkMessage(byte[] buf,
                                               NetworkPDU npdu,
                                               NetworkMessagePDU pdu)
    {
        OutputBuffer output = new OutputBuffer();
        npdu.encode(output);
        pdu.encodeHeader(output);

        byte[] targetBuf = output.toByteArray();
        byte[] sourceBuf = Arrays.copyOfRange(buf, 4, buf.length);

        if (comparePayloads(targetBuf, sourceBuf, targetBuf.length))
        {
            if (m_verbose)
            {
                System.out.println("PERFECT MATCH!");
            }
        }
    }

    //--//

    private void testSweep(byte[] buf,
                           int length)
    {
        while (length < buf.length)
        {
            if (verboseSweep)
            {
                System.out.println();
                System.out.println(String.format("Trying to test buffer truncated %d bytes early", buf.length - length));
                System.out.println();
            }

            try (LoggerResource config = LoggerFactory.pushPerThreadConfig())
            {
                try
                {
                    TagContextForDecoding.LoggerInstance.disablePerThread(Severity.Debug);
                    TagContextForEncoding.LoggerInstance.disablePerThread(Severity.Debug);

                    InputBuffer    ib  = InputBuffer.createFrom(buf, 0, length);
                    ApplicationPDU pdu = ApplicationPDU.decodeHeader(ib);
                    Object         res = pdu.decodePayload();
                    fail("Unexpected success");
                    assertNotNull(res);
                }
                catch (Exception e)
                {
                    if (verboseSweep)
                    {
                        System.out.println(e.toString());
                        e.printStackTrace();
                    }
                }
            }

            length++;
        }
    }

    private byte[] parseHex(String[] lines)
    {
        OutputBuffer ob = new OutputBuffer();

        for (String line : lines)
        {
            parseHex(ob, line);
        }

        return ob.toByteArray();
    }

    private void parseHex(OutputBuffer ob,
                          String line)
    {
        String[] parts = line.trim()
                             .split(" ");

        for (String part : parts)
        {
            int digitHi = Character.digit(part.charAt(0), 16);
            int digitLo = Character.digit(part.charAt(1), 16);

            if (digitHi < 0 || digitLo < 0)
            {
                break;
            }

            ob.emit1Byte((digitHi << 4) | digitLo);
        }
    }

    private byte[] parse(String[] lines)
    {
        OutputBuffer ob = new OutputBuffer();

        for (String line : lines)
        {
            String line2 = line.trim();
            if (line2.startsWith("X'"))
            {
                if (m_verbose)
                {
                    System.out.println(String.format("%04d : %s", ob.size(), line));
                }

                for (int pos = 2; pos + 2 <= line2.length(); )
                {
                    int digitHi = Character.digit(line2.charAt(pos++), 16);
                    int digitLo = Character.digit(line2.charAt(pos++), 16);

                    if (digitHi < 0 || digitLo < 0)
                    {
                        break;
                    }

                    ob.emit1Byte((digitHi << 4) | digitLo);
                }
            }
        }

        return ob.toByteArray();
    }

    private void dumpByteArray(byte[] buf)
    {
        if (m_verbose)
        {
            System.out.println();
            System.out.println("Dump of buffer:");
            BufferUtils.convertToHex(buf, 0, buf.length, 32, true, System.out::println);
        }
    }
}
