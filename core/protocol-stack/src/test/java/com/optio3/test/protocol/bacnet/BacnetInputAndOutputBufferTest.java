/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.bacnet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.BitSet;

import com.optio3.protocol.bacnet.model.pdu.TagHeaderForDecoding;
import com.optio3.protocol.bacnet.model.pdu.TagHeaderForEncoding;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.test.common.Optio3Test;
import org.junit.Test;

public class BacnetInputAndOutputBufferTest extends Optio3Test
{
    @Test
    public void basicTests()
    {
        OutputBuffer ob = new OutputBuffer();

        BitSet bs = new BitSet();
        bs.set(9);
        bs.set(0);

        TagHeaderForEncoding.emitAsBitString(ob, bs);

        byte[] buf = ob.toByteArray();

        InputBuffer ib = new InputBuffer(ob);
        assertNull(TagHeaderForDecoding.readString(ib, 0));
        BitSet bs2 = TagHeaderForDecoding.readBitString(ib, -1);
        assertEquals(bs, bs2);

        assertEquals(6, buf[0]);
        assertEquals(128, buf[1] & 0xFF);
        assertEquals(0x40, buf[2] & 0xFF);

        final String testText = "Test of UTF8";
        ob.reset();
        TagHeaderForEncoding.emitString(ob, testText, null);

        ib = new InputBuffer(ob);
        String str = TagHeaderForDecoding.readString(ib, -1);
        assertEquals(testText, str);

        assertFailure(RuntimeException.class, () ->
        {
            OutputBuffer obBad = new OutputBuffer();
            obBad.emit1Byte(0x44);
            obBad.emit(new byte[] { 0x12, 0x23, 0x45 });

            InputBuffer ibBad = InputBuffer.createFrom(obBad.toByteArray(), 0, 4);
            TagHeaderForDecoding.readString(ibBad, -1);
        });
    }
}
