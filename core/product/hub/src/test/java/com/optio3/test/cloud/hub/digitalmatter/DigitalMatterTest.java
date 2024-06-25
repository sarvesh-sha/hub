/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub.digitalmatter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;

import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.customization.digitalmatter.InstanceConfigurationForDigitalMatter;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseWireModel;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.Packet;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.async.ConnectToOemAsync;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field.GpsDataField;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.AsyncMessageRequestPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.CommitRequestPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.CommitResponsePayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.HelloPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.HelloResponsePayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.SendDataRecordPayload;
import com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message.SendDataRecordsPayload;
import com.optio3.serialization.ObjectMappers;
import com.optio3.stream.OutputBuffer;
import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

public class DigitalMatterTest
{
    @Test
    public void testHello() throws
                            Exception
    {
        byte[] buf = parseHex(new String[] { "02 55 00 31 00",
                                             "AC 86 01 00 33 35 31 37 33 32 30 35 30 38 37 35 32 30 35 00 38 39 35 32 34 36 30 30 30 30 30 30 30 30 35 34 30 39 30 33 00",
                                             "11 01 02 0E 00 00 00 00" });

        Packet packet = decodeFromBuffer(buf);
        assertEquals(Packet.MessageType.Hello.forRequest(), packet.messageType);
        HelloPayload payload = (HelloPayload) packet.payload;
        assertEquals(100012, payload.deviceSerial.unbox());

        roundtrip(buf, payload);
    }

    @Test
    public void testHelloResponse() throws
                                    Exception
    {
        byte[] buf = parseHex(new String[] { "02 55 01 08 00", "67 7C 37 02 00 00 00 00" });

        Packet packet = decodeFromBuffer(buf);
        assertEquals(Packet.MessageType.HelloResponse.forRequest(), packet.messageType);
        HelloResponsePayload payload = (HelloResponsePayload) packet.payload;
        assertEquals("2014-03-07T10:45:59Z", payload.timestamp.toString());

        roundtrip(buf, payload);
    }

    @Test
    public void testDataRecords() throws
                                  Exception
    {
        byte[] buf = parseHex(new String[] { "02 55",
                                             "04",
                                             "3D 00",
                                             "3D 00",
                                             "47 46 00 00",
                                             "96 D6 84 02",
                                             "0B",
                                             "00 15",
                                             "02 D4 84 02 F0 43 F4 EC 2A 69 09 45 2B 00 1F 00 05 00 11 23 03",
                                             "02 08",
                                             "00 00 00 00 00 00 0A 00",
                                             "06 0F",
                                             "04 1D 00 01 FE 0F 02 1E 00 05 00 00 03 BF 08", });

        Packet packet = decodeFromBuffer(buf);
        assertEquals(Packet.MessageType.SendDataRecords.forRequest(), packet.messageType);
        SendDataRecordsPayload payload = (SendDataRecordsPayload) packet.payload;
        assertEquals(1, payload.records.length);

        SendDataRecordPayload record = payload.records[0];
        assertEquals(3, record.payload.size());
        GpsDataField field1 = (GpsDataField) record.payload.get(0).field;
        assertEquals(43, field1.altitude);

        roundtrip(buf, payload);
    }

    @Test
    public void testAsyncRequest() throws
                                   Exception
    {
        byte[] buf = parseHex(new String[] { "02 55 20 18 00 15 6A 00 00 FF FF FF FF 08 00 00 E0 94 5D 09 DC 05 03 00 DA 0A 53 F0 1B" });

        Packet packet = decodeFromBuffer(buf);
        assertEquals(Packet.MessageType.AsyncMessageRequest.forRequest(), packet.messageType);
        AsyncMessageRequestPayload payload = (AsyncMessageRequestPayload) packet.payload;

        roundtrip(buf, payload);
    }

    @Test
    public void testAsyncRequest2() throws
                                    Exception
    {
        byte[] buf = parseHex(new String[] { "02 55 20 0D 00 01 00 00 00 FF FF FF FF 07 00 00 00 00" });

        Packet packet = decodeFromBuffer(buf);
        assertEquals(Packet.MessageType.AsyncMessageRequest.forRequest(), packet.messageType);
        AsyncMessageRequestPayload payload  = (AsyncMessageRequestPayload) packet.payload;
        ConnectToOemAsync          payload2 = (ConnectToOemAsync) payload.payload;
        assertNotNull(payload2);

        roundtrip(buf, payload);
    }

    @Test
    public void testCommitRequest() throws
                                    Exception
    {
        byte[] buf = parseHex(new String[] { "02 55 05 00 00" });

        Packet packet = decodeFromBuffer(buf);
        assertEquals(Packet.MessageType.CommitRequest.forRequest(), packet.messageType);
        CommitRequestPayload payload = (CommitRequestPayload) packet.payload;
        assertNotNull(payload);

        roundtrip(buf, payload);
    }

    @Test
    public void testCommitResponse() throws
                                     Exception
    {
        byte[] buf = parseHex(new String[] { "02 55 06 01 00 01" });

        Packet packet = decodeFromBuffer(buf);
        assertEquals(Packet.MessageType.CommitResponse.forRequest(), packet.messageType);
        CommitResponsePayload payload = (CommitResponsePayload) packet.payload;
        assertNotNull(payload);
        assertTrue(payload.success);

        roundtrip(buf, payload);
    }

    @Test
    public void testDataRecordsFromRealDevice() throws
                                                Exception
    {
        byte[] buf = parseHex(new String[] { "02 55 04 C1 01 1F 00 1E 00 00 00 80 B3 09 0D 15 01 12 0E 00 45 78 69 74 69 6E 67 20 47 70 73 20",
                                             "4D 6F 64 65 25 00 1F 00 00 00 86 B3 09 0D 15 01 18 03 00 52 45 53 45 54 20 4F 79 73 74 65 72 32",
                                             "20 37 37 2E 31 2E 33 2E 35 3A 00 20 00 00 00 86 B3 09 0D 15 01 2D 03 00 56 42 61 74 20 3D 20 34",
                                             "31 38 35 20 6D 56 2C 20 52 65 73 65 74 20 3D 20 30 58 34 2C 20 57 61 74 63 68 64 6F 67 20 3D 20",
                                             "30 58 31 25 00 21 00 00 00 86 B3 09 0D 15 01 18 0E 00 45 6E 74 65 72 69 6E 67 20 52 65 63 6F 76",
                                             "65 72 79 20 4D 6F 64 65 3A 00 22 00 00 00 86 B3 09 0D 24 00 15 00 00 00 00 00 00 00 00 00 00 00",
                                             "00 00 00 00 00 00 00 00 00 00 02 08 02 00 00 00 00 00 82 00 06 0C 01 59 10 03 F1 0A 05 59 10 06",
                                             "0E 27 3A 00 23 00 00 00 38 B4 09 0D 0B 00 15 37 B4 09 0D 86 04 7D 1C 39 5D 25 B7 59 00 00 00 04",
                                             "00 11 08 03 02 08 02 00 00 00 00 00 82 00 06 0C 01 59 10 03 F9 0A 05 58 10 06 0A 27 24 00 24 00",
                                             "00 00 90 B6 09 0D 15 01 17 0A 00 41 62 6F 72 74 69 6E 67 20 6F 6E 65 73 68 6F 74 20 64 61 74 61",
                                             "3D 00 26 00 00 00 89 BB 09 0D 0B 00 15 88 BB 09 0D C4 FF 7C 1C 4C 62 25 B7 6B 00 00 00 02 00 0D",
                                             "0A 03 02 08 02 00 00 00 00 00 82 00 06 0F 01 5F 10 03 2C 0B 04 0F 00 05 53 10 06 FF 26 24 00 27",
                                             "00 00 00 E0 BD 09 0D 15 01 17 0A 00 41 62 6F 72 74 69 6E 67 20 6F 6E 65 73 68 6F 74 20 64 61 74",
                                             "61 25 00 28 00 00 00 00 00 00 00 15 01 18 03 00 52 45 53 45 54 20 4F 79 73 74 65 72 32 20 37 37",
                                             "2E 31 2E 33 2E 35" });

        Packet packet = decodeFromBuffer(buf);
        assertEquals(Packet.MessageType.SendDataRecords.forRequest(), packet.messageType);
        SendDataRecordsPayload payload = (SendDataRecordsPayload) packet.payload;

        roundtrip(buf, payload);
    }

    @Test
    public void testDataRecordsFromRealDevice2() throws
                                                 Exception
    {
        byte[] buf = parseHex(new String[] { "02 55 04 A7 00 3D 00 E3 00 00 00 04 07 0B 0D 0B 00 15 04 07 0B 0D 45 FC 7C 1C F4 5D 25 B7 74 00",
                                             "03 00 07 00 10 27 03 02 08 02 00 00 00 00 00 02 00 06 0F 01 70 10 03 6E 0A 04 12 00 05 56 10 06",
                                             "49 25 6A 00 E4 00 00 00 06 07 0B 0D 2A 15 5D 00 70 10 01 49 25 02 8C 00 09 00 00 03 B5 0A 04 55",
                                             "10 80 00 00 00 00 81 00 00 00 00 82 00 00 00 00 83 00 00 00 00 84 01 00 00 00 85 0C 00 00 00 86",
                                             "00 00 00 00 87 00 00 00 00 88 00 00 00 00 89 00 00 00 00 8A 00 00 00 00 8B 00 00 00 00 8C 00 00",
                                             "00 00 8D 00 00 00 00 8E 52 16 01 00" });

        Packet packet = decodeFromBuffer(buf);
        assertEquals(Packet.MessageType.SendDataRecords.forRequest(), packet.messageType);
        SendDataRecordsPayload payload = (SendDataRecordsPayload) packet.payload;

        roundtrip(buf, payload);
    }

    @Ignore("Disabled because it requires network connectivity")
    @Test
    public void testConnection() throws
                                 Exception
    {
//        Socket socket = new Socket("localhost", InstanceConfigurationForDigitalMatter.TCP_PORT);
//        Socket socket = new Socket("test-dm.dev.optio3.io", InstanceConfigurationForDigitalMatter.TCP_PORT);
        Socket socket = new Socket("pilot-lorisystems.optio3.io", InstanceConfigurationForDigitalMatter.TCP_PORT);

        byte[] buf = parseHex(new String[] { "02 55 00 31 00 7D 6F 03 00 33 35 38 30 31 34 30 39 38 39 32 38 39 39 36 00 38 39 30 31 32 36 30",
                                             "38 35 32 32 39 38 32 38 31 38 31 37 00 00 4D 01 03 05 00 00 00 00" });

        Packet packet = decodeFromBuffer(buf);
        send(socket, packet.payload);
        receive(socket);

        byte[] buf2 = parseHex(new String[] { "02 55 04 E5 01 3D 00 F3 00 00 00 5C 21 0B 0D 0B 00 15 5D 21 0B 0D 10 FA 7C 1C 2E 5E 25 B7 8D 00",
                                              "00 00 05 AB 17 22 03 02 08 02 00 00 00 00 00 02 00 06 0F 01 70 10 03 6A 0A 04 13 00 05 58 10 06",
                                              "1E 25 24 00 F4 00 00 00 02 23 0B 0D 15 01 17 0A 00 41 62 6F 72 74 69 6E 67 20 6F 6E 65 73 68 6F",
                                              "74 20 64 61 74 61 3D 00 F5 00 00 00 BE 23 0B 0D 0B 00 15 BE 23 0B 0D 72 FE 7C 1C 2E 64 25 B7 99",
                                              "00 09 00 07 00 10 29 03 02 08 02 00 00 00 00 00 02 00 06 0F 01 70 10 03 9C 0A 04 12 00 05 59 10",
                                              "06 12 25 24 00 F6 00 00 00 64 25 0B 0D 15 01 17 0A 00 41 62 6F 72 74 69 6E 67 20 6F 6E 65 73 68",
                                              "6F 74 20 64 61 74 61 3D 00 F7 00 00 00 1F 26 0B 0D 0B 00 15 1F 26 0B 0D FC FC 7C 1C 85 5D 25 B7",
                                              "90 00 05 00 0A 00 15 27 03 02 08 02 00 00 00 00 00 02 00 06 0F 01 70 10 03 8C 0A 04 12 00 05 59",
                                              "10 06 06 25 24 00 F8 00 00 00 C4 27 0B 0D 15 01 17 0A 00 41 62 6F 72 74 69 6E 67 20 6F 6E 65 73",
                                              "68 6F 74 20 64 61 74 61 3D 00 F9 00 00 00 87 28 0B 0D 0B 00 15 87 28 0B 0D 1B FB 7C 1C 5B 69 25",
                                              "B7 4A 00 0D 00 0C 00 17 2B 03 02 08 02 00 00 00 00 00 02 00 06 0F 01 70 10 03 9A 0A 04 12 00 05",
                                              "59 10 06 FA 24 24 00 FA 00 00 00 2D 2A 0B 0D 15 01 17 0A 00 41 62 6F 72 74 69 6E 67 20 6F 6E 65",
                                              "73 68 6F 74 20 64 61 74 61 3D 00 FB 00 00 00 EF 2A 0B 0D 0B 00 15 EF 2A 0B 0D D2 F0 7C 1C F3 6C",
                                              "25 B7 46 00 08 00 0A 48 15 2B 03 02 08 02 00 00 00 00 00 02 00 06 0F 01 70 10 03 9B 0A 04 12 00",
                                              "05 58 10 06 EE 24 24 00 FC 00 00 00 95 2C 0B 0D 15 01 17 0A 00 41 62 6F 72 74 69 6E 67 20 6F 6E",
                                              "65 73 68 6F 74 20 64 61 74 61" });
        packet = decodeFromBuffer(buf2);
        send(socket, packet.payload);

        byte[] buf3 = parseHex(new String[] { "02 55 04 A7 00 3D 00 F8 01 00 00 95 5A 0C 0D 0B 00 15 96 5A 0C 0D 1F 02 7D 1C 31 5D 25 B7 91 00",
                                              "00 00 05 00 19 1E 03 02 08 02 00 00 00 00 00 02 00 06 0F 01 79 10 03 66 0A 04 12 00 05 5A 10 06",
                                              "E3 1F 6A 00 F9 01 00 00 97 5A 0C 0D 2A 15 5D 00 79 10 01 E3 1F 02 39 02 09 00 00 03 C3 0A 04 55",
                                              "10 80 08 00 00 00 81 CF 01 00 00 82 86 00 00 00 83 C1 DD 00 00 84 8F 00 00 00 85 BA 05 00 00 86",
                                              "00 00 00 00 87 00 00 00 00 88 00 00 00 00 89 00 00 00 00 8A 00 00 00 00 8B 00 00 00 00 8C 00 00",
                                              "00 00 8D 00 00 00 00 8E EC 69 02 00" });
        packet = decodeFromBuffer(buf3);
        send(socket, packet.payload);

        send(socket, new CommitRequestPayload());
        receive(socket);

        socket.close();
    }

    //--//

    private void send(Socket socket,
                      BaseWireModel model) throws
                                           IOException
    {
        Packet packet = new Packet();
        packet.payload = model;
        socket.getOutputStream()
              .write(packet.encode());
    }

    private BaseWireModel receive(Socket socket) throws
                                                 IOException
    {
        Packet packet = Packet.decode(HubApplication.LoggerInstance, socket.getInputStream());
        return packet != null ? packet.payload : null;
    }

    private Packet decodeFromBuffer(byte[] buf) throws
                                                IOException
    {
        Packet packet = Packet.decode(HubApplication.LoggerInstance, new ByteArrayInputStream(buf));
        HubApplication.LoggerInstance.info(ObjectMappers.prettyPrintAsJson(packet));
        return packet;
    }

    private void roundtrip(byte[] buf,
                           BaseWireModel obj)
    {
        Packet packet = new Packet();
        packet.payload = obj;

        assertArrayEquals(buf, packet.encode());
    }

    private byte[] parseHex(String[] lines)
    {
        try (OutputBuffer ob = new OutputBuffer())
        {
            for (String line : lines)
            {
                parseHex(ob, line);
            }

            return ob.toByteArray();
        }
    }

    private void parseHex(OutputBuffer ob,
                          String line)
    {
        String[] parts = StringUtils.split(line, " ");

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
}
