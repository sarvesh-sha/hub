/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.remoting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import com.google.common.collect.Lists;
import com.optio3.cloud.JsonDatagram;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.test.common.Optio3Test;
import org.junit.Ignore;
import org.junit.Test;

public class DatagramTest extends Optio3Test
{
    @Test
    public void testEncoding()
    {
        {
            JsonDatagram.PublicHeader obj = new JsonDatagram.PublicHeader();
            obj.version     = 1;
            obj.transportId = 2345;

            OutputBuffer ob = JsonDatagram.toOutputBuffer(obj);
            assertEquals(JsonDatagram.PublicHeader.SIZE, ob.size());
            JsonDatagram.PublicHeader obj2 = new JsonDatagram.PublicHeader();
            obj2 = JsonDatagram.fromInputBuffer(new InputBuffer(ob), obj2);

            assertNotNull(obj2);
            assertEquals(obj.version, obj2.version);
            assertEquals(obj.transportId, obj2.transportId);
        }

        {
            JsonDatagram.SharedHeader obj = new JsonDatagram.SharedHeader();
            obj.sessionId   = 1234;
            obj.frameLength = 456;

            OutputBuffer ob = JsonDatagram.toOutputBuffer(obj);
            assertEquals(JsonDatagram.SharedHeader.SIZE, ob.size());
            JsonDatagram.SharedHeader obj2 = new JsonDatagram.SharedHeader();
            obj2 = JsonDatagram.fromInputBuffer(new InputBuffer(ob), obj2);

            assertNotNull(obj2);
            assertEquals(obj.sessionId, obj2.sessionId);
            assertEquals(obj.frameLength, obj2.frameLength);
        }

        {
            JsonDatagram.FragmentHeader obj = new JsonDatagram.FragmentHeader();
            obj.length = 1234;
            obj.ack    = true;

            OutputBuffer ob = JsonDatagram.toOutputBuffer(obj);
            assertEquals(JsonDatagram.FragmentHeader.SIZE, ob.size());
            JsonDatagram.FragmentHeader obj2 = new JsonDatagram.FragmentHeader();
            obj2 = JsonDatagram.fromInputBuffer(new InputBuffer(ob), obj2);

            assertNotNull(obj2);
            assertEquals(obj.length, obj2.length);
            assertEquals(obj.ack, obj2.ack);
        }

        {
            JsonDatagram.DataPayload obj = new JsonDatagram.DataPayload();
            obj.messageId      = 234;
            obj.fragmentOffset = 123;
            obj.data           = new byte[] { 1, 2 };

            OutputBuffer ob = JsonDatagram.toOutputBuffer(obj);
            assertEquals(JsonDatagram.DataPayload.SIZE + 2, ob.size());
            JsonDatagram.DataPayload obj2 = new JsonDatagram.DataPayload();
            obj2 = JsonDatagram.fromInputBuffer(new InputBuffer(ob), obj2);

            assertNotNull(obj2);
            assertEquals(obj.messageId, obj2.messageId);
            assertEquals(obj.fragmentOffset, obj2.fragmentOffset);
            assertArrayEquals(obj.data, obj2.data);
        }

        {
            JsonDatagram.AckRange range = new JsonDatagram.AckRange();
            range.rangeStart = 123;
            range.rangeEnd   = 234;

            JsonDatagram.AckPayload obj = new JsonDatagram.AckPayload();
            obj.messageId = 234;
            obj.ranges    = Lists.newArrayList();
            obj.ranges.add(range);

            OutputBuffer ob = JsonDatagram.toOutputBuffer(obj);
            assertEquals(JsonDatagram.AckPayload.SIZE + JsonDatagram.AckRange.SIZE, ob.size());
            JsonDatagram.AckPayload obj2 = new JsonDatagram.AckPayload();
            obj2 = JsonDatagram.fromInputBuffer(new InputBuffer(ob), obj2);

            assertNotNull(obj2);
            assertEquals(obj.messageId, obj2.messageId);
            assertEquals(obj.ranges.size(), obj2.ranges.size());

            JsonDatagram.AckRange range2 = obj2.ranges.get(0);
            assertEquals(range2.rangeStart, range.rangeStart);
            assertEquals(range2.rangeEnd, range.rangeEnd);
        }
    }

    @Ignore("Manually enable to test sending a UDP packet to a server")
    @Test
    public void testUDP() throws
                          IOException
    {
        DatagramSocket socket = new DatagramSocket();

        byte[] data = new byte[] { 0x01, 0x02, 0x03 };

        InetAddress    ia = InetAddress.getByName("localhost");
        DatagramPacket p  = new DatagramPacket(data, data.length, ia, 20443);
        socket.send(p);
    }
}
