/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.stream;

import static org.junit.Assert.assertEquals;

import java.util.BitSet;

import com.optio3.stream.InputBitBuffer;
import com.optio3.stream.OutputBitBuffer;
import com.optio3.test.common.Optio3Test;
import org.junit.Test;

public class InputAndOutputBitBufferTest extends Optio3Test
{
    @Test
    public void testFixedLength()
    {
        final byte[] c_sample  = { 0x00, 0x00, 0x00, 8 + 16 + 24 + 16, 0x12, 0x23, 0x34, 0x45, 0x56, 0x67, 0x78, (byte) 0x89 };
        final String c_sample2 = "This is a test of text encoding";

        OutputBitBuffer ob = new OutputBitBuffer();

        ob.emitFixedLength(0x12, 8);
        ob.emitFixedLength(0x2334, 16);
        ob.emitFixedLength(0x455667, 24);
        ob.emitFixedLength(0x7889, 16);

        assertArrayEquals(c_sample, ob.toByteArray());
        ob.emitFloat(128.4f);
        ob.emitDouble(-1230.4);

        ob.emitByteArray(c_sample);
        ob.emitString(c_sample2);

        BitSet bs = new BitSet();
        bs.set(1);
        bs.set(129);
        ob.emitBitSet(bs);

        InputBitBuffer ib = new InputBitBuffer(ob);
        assertEquals(0x12, ib.readSignedFixedLength(8));
        assertEquals(0x2334, ib.readSignedFixedLength(16));
        assertEquals(0x455667, ib.readSignedFixedLength(24));
        assertEquals(0x7889, ib.readSignedFixedLength(16));
        assertEquals(128.4f, ib.readFloat(), 0);
        assertEquals(-1230.4, ib.readDouble(), 0);
        assertArrayEquals(c_sample, ib.readByteArray());
        assertEquals(c_sample2, ib.readString());
        assertEquals(bs, ib.readBitSet());
    }

    @Test
    public void testVariableLength()
    {
        OutputBitBuffer ob  = new OutputBitBuffer();
        int             len = 0;

        ob.emitUnsignedVariableLength(0b0);
        len += 2 + 3;
        assertEquals(len, ob.sizeInBits());

        ob.emitUnsignedVariableLength(0b111);
        len += 2 + 3;
        assertEquals(len, ob.sizeInBits());

        ob.emitUnsignedVariableLength(0b1111);
        len += 2 + 5;
        assertEquals(len, ob.sizeInBits());

        ob.emitUnsignedVariableLength(0b1_1111);
        len += 2 + 5;
        assertEquals(len, ob.sizeInBits());

        ob.emitUnsignedVariableLength(0b11_1111);
        len += 3 + 7;
        assertEquals(len, ob.sizeInBits());

        ob.emitUnsignedVariableLength(0b111_1111);
        len += 3 + 7;
        assertEquals(len, ob.sizeInBits());

        ob.emitUnsignedVariableLength(0b1111_1111);
        len += 3 + 12;
        assertEquals(len, ob.sizeInBits());

        ob.emitUnsignedVariableLength(0b1111_1111_1111);
        len += 3 + 12;
        assertEquals(len, ob.sizeInBits());

        ob.emitUnsignedVariableLength(0b1_1111_1111_1111);
        len += 2 + 4 + 13;
        assertEquals(len, ob.sizeInBits());

        ob.emitUnsignedVariableLength(0b11_1111_1111_1111);
        len += 2 + 4 + 17;
        assertEquals(len, ob.sizeInBits());

        ob.emitUnsignedVariableLength(1L << 15);
        len += 2 + 4 + 17;
        assertEquals(len, ob.sizeInBits());

        ob.emitUnsignedVariableLength(1L << 31);
        len += 2 + 4 + 33;
        assertEquals(len, ob.sizeInBits());

        ob.emitUnsignedVariableLength(1L << 34);
        len += 2 + 4 + 37;
        assertEquals(len, ob.sizeInBits());

        for (int i = 0; i < 63; i++)
        {
            ob.emitUnsignedVariableLength(1L << i);
        }

        InputBitBuffer ib = new InputBitBuffer(ob);
        assertEquals(0b0, ib.readUnsignedVariableLength());
        assertEquals(0b111, ib.readUnsignedVariableLength());
        assertEquals(0b1111, ib.readUnsignedVariableLength());
        assertEquals(0b1_1111, ib.readUnsignedVariableLength());
        assertEquals(0b11_1111, ib.readUnsignedVariableLength());
        assertEquals(0b111_1111, ib.readUnsignedVariableLength());
        assertEquals(0b1111_1111, ib.readUnsignedVariableLength());
        assertEquals(0b1111_1111_1111, ib.readUnsignedVariableLength());
        assertEquals(0b1_1111_1111_1111, ib.readUnsignedVariableLength());
        assertEquals(0b11_1111_1111_1111, ib.readUnsignedVariableLength());
        assertEquals(1L << 15, ib.readUnsignedVariableLength());
        assertEquals(1L << 31, ib.readUnsignedVariableLength());
        assertEquals(1L << 34, ib.readUnsignedVariableLength());

        for (int i = 0; i < 63; i++)
        {
            assertEquals(1L << i, ib.readUnsignedVariableLength());
        }

        //--//

        ob.reset();
        len = 0;

        ob.emitSignedVariableLength(0);
        len += 1 + 2 + 3;
        assertEquals(len, ob.sizeInBits());

        ob.emitSignedVariableLength(7);
        len += 1 + 2 + 3;
        assertEquals(len, ob.sizeInBits());

        ob.emitSignedVariableLength(-7);
        len += 1 + 2 + 3;
        assertEquals(len, ob.sizeInBits());

        ob.emitSignedVariableLength(-8);
        len += 1 + 2 + 5;
        assertEquals(len, ob.sizeInBits());

        ob.emitSignedVariableLength(31);
        len += 1 + 2 + 5;
        assertEquals(len, ob.sizeInBits());

        ob.emitSignedVariableLength(-31);
        len += 1 + 2 + 5;
        assertEquals(len, ob.sizeInBits());

        ob.emitSignedVariableLength(127);
        len += 1 + 2 + 8;
        assertEquals(len, ob.sizeInBits());

        ob.emitSignedVariableLength(-128);
        len += 1 + 3 + 12;
        assertEquals(len, ob.sizeInBits());

        ob.emitSignedVariableLength(-1L << 15);
        len += 1 + 2 + 4 + 17;
        assertEquals(len, ob.sizeInBits());

        ob.emitSignedVariableLength(-1L << 31);
        len += 1 + 2 + 4 + 33;
        assertEquals(len, ob.sizeInBits());

        ob.emitSignedVariableLength(-1L << 34);
        len += 1 + 2 + 4 + 37;
        assertEquals(len, ob.sizeInBits());

        for (int i = 0; i < 62; i++)
        {
            ob.emitSignedVariableLength(-1L << i);
        }

        ib = new InputBitBuffer(ob);
        assertEquals(0, ib.readSignedVariableLength());
        assertEquals(7, ib.readSignedVariableLength());
        assertEquals(-7, ib.readSignedVariableLength());
        assertEquals(-8, ib.readSignedVariableLength());
        assertEquals(31, ib.readSignedVariableLength());
        assertEquals(-31, ib.readSignedVariableLength());
        assertEquals(127, ib.readSignedVariableLength());
        assertEquals(-128, ib.readSignedVariableLength());
        assertEquals(-1L << 15, ib.readSignedVariableLength());
        assertEquals(-1L << 31, ib.readSignedVariableLength());
        assertEquals(-1L << 34, ib.readSignedVariableLength());

        for (int i = 0; i < 62; i++)
        {
            assertEquals(-1L << i, ib.readSignedVariableLength());
        }
    }

    @Test
    public void testNested()
    {
        OutputBitBuffer ob = new OutputBitBuffer();

        ob.emitFixedLength(0x12345, 64);
        ob.emitFixedLength(0x12345, 17);

        OutputBitBuffer obSub = new OutputBitBuffer();
        for (int i = 0; i < 15; i++)
        {
            obSub.emitFixedLength(123 + i, 14);
        }

        ob.emitNested(obSub);

        InputBitBuffer ib = new InputBitBuffer(ob);
        assertEquals(0x12345L, ib.readUnsignedFixedLength(64));
        assertEquals(0x12345L, ib.readUnsignedFixedLength(17));
        for (int i = 0; i < 15; i++)
        {
            assertEquals(123 + i, ib.readUnsignedFixedLength(14));
        }
    }

    @Test
    public void testAllVariousAlignments()
    {
        for (int k = 0; k < 64; k++)
        {
            OutputBitBuffer ob = new OutputBitBuffer();

            // We use 4KB pages on the underlying expandable array.
            // Let's move really close to the page break.
            ob.emitByteArray(new byte[4096 - 64]);

            ob.emitFixedLength(0, k);

            for (int i = 1; i < 64; i++)
            {
                ob.emitFixedLength(1L | (1L << (i - 1)), i);
            }

            InputBitBuffer ib = new InputBitBuffer(ob);

            assertEquals(4096 - 64, ib.readByteArray().length);

            ib.readUnsignedFixedLength(k);

            for (int i = 1; i < 64; i++)
            {
                assertEquals(String.format("Failed at k=%d, i=%d", k, i), 1L | (1L << (i - 1)), ib.readUnsignedFixedLength(i));
            }
        }
    }
}
