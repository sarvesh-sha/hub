/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.stream;

import java.util.BitSet;

import com.google.common.base.Charsets;
import com.optio3.serialization.TypeDescriptorKind;
import com.optio3.util.Exceptions;

public class InputBitBuffer implements AutoCloseable
{
    private static final int c_bytesInInt  = 4;
    private static final int c_bytesInLong = 8;
    private static final int c_bitsInByte  = 8;
    private static final int c_bitsInInt   = c_bytesInInt * c_bitsInByte;
    private static final int c_bitsInLong  = c_bytesInLong * c_bitsInByte;

    private final InputBuffer m_ib;
    private final int         m_lengthInBits;
    private       int         m_currentPositionInBits;
    private       long        m_pendingWord;
    private       int         m_pendingBits;

    public InputBitBuffer(OutputBitBuffer ob)
    {
        this(ob.toInputBuffer());
    }

    public InputBitBuffer(InputBuffer ib)
    {
        int length = ib.remainingLength();
        if (length < 4)
        {
            throw new IndexOutOfBoundsException();
        }

        m_ib = ib;

        //
        // All data is written as big-endian.
        //
        ib.littleEndian = false;

        m_lengthInBits = ib.read4BytesSigned();
    }

    @Override
    public void close()
    {
        m_ib.close();
    }

    public long readUnsignedFixedLength(int length)
    {
        if (m_currentPositionInBits + length > m_lengthInBits)
        {
            throw new IndexOutOfBoundsException();
        }

        if (length > c_bitsInLong)
        {
            throw Exceptions.newIllegalArgumentException("Length greater than 64: %d", length);
        }

        long value = 0;

        while (length > 0)
        {
            if (m_pendingBits == 0)
            {
                fetchNextBits();
            }

            int  availableBits = Math.min(m_pendingBits, length);
            long partialWord   = m_pendingWord >>> (c_bitsInLong - availableBits);

            value = (value << availableBits) | partialWord;
            length -= availableBits;

            m_pendingWord <<= availableBits;
            m_pendingBits -= availableBits;
            m_currentPositionInBits += availableBits;
        }

        return value;
    }

    public long readSignedFixedLength(int length)
    {
        long value = readUnsignedFixedLength(length);

        if (length < 64)
        {
            value <<= (64 - length);
            value >>= (64 - length);
        }

        return value;
    }

    public long readUnsignedVariableLength()
    {
        int header = (int) readUnsignedFixedLength(1);
        if (header == BitBufferEncoding.VariableLength3or5)
        {
            header = (header << 1) | (int) readUnsignedFixedLength(1);

            if (header == BitBufferEncoding.VariableLength3)
            {
                return readUnsignedFixedLength(3);
            }
            else
            {
                return readUnsignedFixedLength(5);
            }
        }

        header = (header << 1) | (int) readUnsignedFixedLength(1);
        if (header == BitBufferEncoding.VariableLength7or12)
        {
            header = (header << 1) | (int) readUnsignedFixedLength(1);

            if (header == BitBufferEncoding.VariableLength7)
            {
                return readUnsignedFixedLength(7);
            }
            else
            {
                return readUnsignedFixedLength(12);
            }
        }

        assert header == BitBufferEncoding.VariableLengthN;

        int variableLength          = (int) readUnsignedFixedLength(4);
        int numberOfSignificantBits = variableLength * 4 + 13;

        //
        // For legacy reasons, we encode some large numbers with more than 64 bits, due to rounding.
        // 'readUnsignedFixedLength()' does not handle more than 64 bits of length.
        // So we have to pull an extra bit, just for backward compatibility.
        //
        if (numberOfSignificantBits > 64)
        {
            readUnsignedFixedLength(numberOfSignificantBits - 64);
            numberOfSignificantBits = 64;
        }

        return readUnsignedFixedLength(numberOfSignificantBits);
    }

    public long readSignedVariableLength()
    {
        boolean negative = readUnsignedFixedLength(1) != 0;
        long    value    = readUnsignedVariableLength();

        return negative ? -value : value;
    }

    public double readDouble()
    {
        return Double.longBitsToDouble(readSignedFixedLength(64));
    }

    public float readFloat()
    {
        return Float.intBitsToFloat((int) readSignedFixedLength(32));
    }

    public BitSet readBitSet()
    {
        BitSet bs = new BitSet();

        readBitSet(bs);

        return bs;
    }

    public void readBitSet(BitSet bs)
    {
        int len = (int) readUnsignedVariableLength();

        bs.clear();

        for (int idx = 0; idx < len; )
        {
            int chunk = Math.min(64, len - idx);

            long word = readUnsignedFixedLength(chunk);
            word <<= 64 - chunk; // Left-align;

            while (word != 0 && chunk > 0)
            {
                if (word < 0) // Top bit is set.
                {
                    bs.set(idx);
                }

                word <<= 1;
                idx++;
                chunk--;
            }

            idx += chunk;
        }
    }

    public byte[] readByteArray()
    {
        int len = (int) readUnsignedVariableLength();

        if (m_currentPositionInBits + len * 8 > m_lengthInBits)
        {
            throw new IndexOutOfBoundsException();
        }

        byte[] buf = new byte[len];

        for (int i = 0; i < len; i++)
        {
            int  valueBits = 8;
            long value     = 0;

            while (valueBits > 0)
            {
                if (m_pendingBits == 0)
                {
                    fetchNextBits();
                }

                int  availableBits = Math.min(m_pendingBits, valueBits);
                long partialWord   = m_pendingWord >>> (c_bitsInLong - availableBits);

                value = (value << availableBits) | partialWord;
                valueBits -= availableBits;

                m_pendingWord <<= availableBits;
                m_pendingBits -= availableBits;
                m_currentPositionInBits += availableBits;
            }

            buf[i] = (byte) value;
        }

        return buf;
    }

    public String readString()
    {
        return new String(readByteArray(), Charsets.UTF_8);
    }

    //--//

    public int sizeInBits()
    {
        return m_lengthInBits;
    }

    public int positionInBits()
    {
        return m_currentPositionInBits;
    }

    //--//

    private void fetchNextBits()
    {
        int remainingBytes = m_ib.remainingLength();
        if (remainingBytes >= c_bytesInLong)
        {
            m_pendingBits = c_bitsInLong;
            m_pendingWord = m_ib.readGenericInteger(c_bytesInLong, TypeDescriptorKind.integerUnsigned);
        }
        else if (remainingBytes >= c_bytesInInt)
        {
            m_pendingBits = Math.min(c_bitsInInt, m_lengthInBits - m_currentPositionInBits);
            m_pendingWord = m_ib.readGenericInteger(c_bytesInInt, TypeDescriptorKind.integerUnsigned) << (c_bitsInLong - c_bitsInInt); // Left align remaining bits.
        }
        else
        {
            m_pendingBits = Math.min(c_bitsInByte, m_lengthInBits - m_currentPositionInBits);
            m_pendingWord = m_ib.readGenericInteger(1, TypeDescriptorKind.integerUnsigned) << (c_bitsInLong - c_bitsInByte); // Left align remaining bits.
        }
    }
}
