/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;

import com.optio3.serialization.TypeDescriptorKind;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.test.common.Optio3Test;
import org.junit.Test;

public class InputAndOutputBufferTest extends Optio3Test
{
    static final byte[] c_sample_BigEndian    = { 0x12, 0x23, 0x34, 0x45, 0x56, 0x67, 0x78, (byte) 0x89 };
    static final byte[] c_sample_LittleEndian = { 0x12, 0x34, 0x23, 0x67, 0x56, 0x45, (byte) 0x89, 0x78 };

    @Test
    public void basicTests() throws
                             IOException
    {
        OutputBuffer ob = new OutputBuffer();

        ob.emit1Byte(0x12);
        ob.emit2Bytes(0x2334);
        ob.emit3Bytes(0x455667);

        ob.emit2Bytes(0x7889);

        assertArrayEquals(c_sample_BigEndian, ob.toByteArray());

        OutputBuffer ob_little = new OutputBuffer();
        ob_little.littleEndian = true;

        ob_little.emit1Byte(0x12);
        ob_little.emit2Bytes(0x2334);
        ob_little.emit3Bytes(0x455667);

        ob_little.emit2Bytes(0x7889);

        assertArrayEquals(c_sample_LittleEndian, ob_little.toByteArray());

        //--//

        InputBuffer ib = InputBuffer.createFrom(c_sample_BigEndian);
        assertEquals(0x12, ib.peekNextByte());

        //
        // Test big-endian alignment
        //
        assertEquals(0x12, ib.read1ByteUnsigned());
        assertEquals(0x2334, ib.read2BytesUnsigned()); // Big-endian
        assertEquals(0x45566778, ib.read4BytesUnsigned()); // Big-endian

        ib.setPosition(1);
        assertEquals(0x233445, ib.read3BytesUnsigned()); // Big-endian

        ib.setPosition(1);
        assertEquals(0x233445, ib.read3BytesSigned()); // Big-endian
        ib.setPosition(6);
        assertEquals(0x7889, ib.read2BytesSigned()); // Big-endian
        assertEquals(8, ib.getPosition());

        InputBuffer ib_little = InputBuffer.createFrom(c_sample_LittleEndian);
        ib_little.littleEndian = true;

        assertEquals(0x12, ib_little.peekNextByte());

        //
        // Test little-endian alignment
        //
        assertEquals(0x12, ib_little.read1ByteUnsigned());
        assertEquals(0x2334, ib_little.read2BytesUnsigned()); // Little-endian
        assertEquals(0x89455667, ib_little.read4BytesUnsigned()); // Little-endian

        ib_little.setPosition(1);
        assertEquals(0x672334, ib_little.read3BytesUnsigned()); // Little-endian

        ib_little.setPosition(1);
        assertEquals(0x672334, ib_little.read3BytesSigned()); // Little-endian
        ib_little.setPosition(6);
        assertEquals(0x7889, ib_little.read2BytesSigned()); // Little-endian
        assertEquals(8, ib_little.getPosition());

        //--//

        //
        // Test signed vs unsigned
        //
        ib.setPosition(7);
        assertEquals(0x89, ib.read1ByteUnsigned());

        ib.setPosition(7);
        assertEquals(-119, ib.read1ByteSigned());

        //
        // Test bit set
        //
        ib.setPosition(0);
        BitSet bs = ib.readU1ByteAsBitSet();
        assertFalse(bs.get(0));
        assertTrue(bs.get(1));
        assertFalse(bs.get(2));
        assertFalse(bs.get(3));
        assertTrue(bs.get(4));
        assertFalse(bs.get(5));

        assertFailure(IndexOutOfBoundsException.class, () ->
        {
            ib.setPosition(-1);
        });

        assertFailure(IndexOutOfBoundsException.class, () ->
        {
            ib.setPosition(ib.size() + 1);
        });

        assertFailure(IndexOutOfBoundsException.class, () ->
        {
            ib.setPosition(ib.size());
            ib.read1ByteUnsigned();
        });

        assertFailure(IndexOutOfBoundsException.class, () ->
        {
            ob.setPosition(-1);
        });

        assertFailure(IndexOutOfBoundsException.class, () ->
        {
            ob.setPosition(ib.size() + 1);
        });

        ob.setPosition(2);
        OutputBuffer obSub = new OutputBuffer();
        obSub.emit4Bytes(0x12345678);
        assertEquals(4, obSub.getPosition());

        ob.emitNestedBlock(obSub);
        assertEquals(6, ob.getPosition());

        byte[] buf1 = Arrays.copyOfRange(ob.toByteArray(), 2, 6);
        byte[] buf2 = obSub.toByteArray();
        assertArrayEquals(buf1, buf2);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ob.toStream(stream, 0, ob.size());

        assertArrayEquals(ob.toByteArray(), stream.toByteArray());

        ob.emit(stream.toByteArray());

        OutputBuffer ob2 = new OutputBuffer();
        ob2.emitAs1Byte(bs);
        byte[] buf3 = ob2.toByteArray();
        assertEquals(1, buf3.length);
        assertEquals(0x12, buf3[0]);

        ob2.reset();
        ob2.emitStringAsBytes("This is a test", null);

        InputBuffer ib3 = new InputBuffer(ob2);
        assertEquals("This is a test", ib3.readBytesAsString(-1, null));
    }

    @Test
    public void floats()
    {
        final float  valueF = 2.3f;
        final double valueD = -14.1;

        OutputBuffer ob = new OutputBuffer();

        ob.emit(valueF);
        ob.emit(valueD);

        assertEquals(12, ob.size());

        InputBuffer ib = new InputBuffer(ob);

        assertEquals(Float.floatToRawIntBits(valueF), ib.read4BytesSigned());
        assertEquals(Double.doubleToRawLongBits(valueD), ib.read8BytesSigned());

        ib.setPosition(0);
        assertTrue(valueF == ib.readFloat());
        assertTrue(valueD == ib.readDouble());
    }

    @Test
    public void largeBuffer()
    {
        OutputBuffer ob = new OutputBuffer();

        for (int i = 0; i < 2048; i++)
        {
            ob.emit1Byte(i);
        }

        byte[] res = ob.toByteArray();
        assertEquals(2048, res.length);

        InputBuffer ib = new InputBuffer(ob);
        for (int i = 0; i < 2048; i++)
        {
            assertEquals(i & 0xFF, ib.read1ByteUnsigned());
        }

        ib.setPosition(500);

        InputBuffer ibSub = ib.readNestedBlock(128);

        assertEquals(500 + 128, ib.getPosition());
        assertEquals(128, ibSub.size());

        for (int i = 500; i < 500 + 128; i++)
        {
            assertEquals(i & 0xFF, ibSub.read1ByteUnsigned());
        }

        byte[] buf = ib.readByteArray(128);
        for (int i = 0; i < 128; i++)
        {
            assertEquals((i + 500 + 128) & 0xFF, buf[i] & 0xFF);
        }
    }

    @Test
    public void variableLengthInteger()
    {
        for (int length = 1; length <= 8; length++)
        {
            checkRountTrip(0x1234_5678_9ABC_DEF0l, length, TypeDescriptorKind.integerSigned);
        }

        for (int length = 1; length <= 8; length++)
        {
            checkRountTrip(0x1234_5678_9ABC_DEF0l, length, TypeDescriptorKind.integerUnsigned);
        }
    }

    private void checkRountTrip(long value,
                                int length,
                                TypeDescriptorKind kind)
    {
        OutputBuffer ob = new OutputBuffer();

        ob.emitGenericInteger(value, length);

        assertEquals(length, ob.size());

        InputBuffer ib     = new InputBuffer(ob);
        long        value2 = ib.readGenericInteger(length, kind);

        int shift = (8 - length) * 8;

        long valueTruncated  = value << shift;
        long value2Truncated = value2 << shift;
        assertTrue(String.format("Mismatch at length %d: %x != %x", length, valueTruncated, value2Truncated), valueTruncated == value2Truncated);

        valueTruncated = kind.signAwareRightShift(valueTruncated, shift);
        assertTrue(String.format("Mismatch at length %d: %x != %x", length, valueTruncated, value2), valueTruncated == value2);
    }

    @Test
    public void testAllVariousAlignments()
    {
        for (int k = 1; k < 8; k++)
        {
            OutputBuffer ob = new OutputBuffer();

            ob.emitGenericInteger(0, k);

            for (int i = 1; i < 8; i++)
            {
                ob.emitGenericInteger(1L | (1L << (8 * i - 1)), i);
            }

            InputBuffer ib = new InputBuffer(ob);

            ib.readGenericInteger(k, TypeDescriptorKind.integerUnsigned);

            for (int i = 1; i < 8; i++)
            {
                assertEquals(String.format("Failed at k=%d, i=%d", k, i), 1L | (1L << (8 * i - 1)), ib.readGenericInteger(i, TypeDescriptorKind.integerUnsigned));
            }
        }
    }
}
