/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.BitSet;

import com.google.common.base.Charsets;
import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.serialization.TypeDescriptorKind;
import com.optio3.util.Exceptions;

public class OutputBitBuffer implements AutoCloseable
{
    private static final int c_bytesInLong = 8;
    private static final int c_bitsInLong  = c_bytesInLong * 8;
    private static final int c_headerSize  = 4;

    private final ExpandableArrayOfBytes m_data;
    private       long                   m_currentWord;
    private       int                    m_lengthInBits;

    public OutputBitBuffer()
    {
        m_data = ExpandableArrayOfBytes.create();

        reset();
    }

    @Override
    public void close()
    {
        m_data.close();
    }

    public void emitNested(OutputBitBuffer other)
    {
        int words         = other.m_lengthInBits / c_bitsInLong;
        int remainingBits = other.m_lengthInBits % c_bitsInLong;

        for (int i = 0; i < words; i++)
        {
            long word = other.m_data.getGenericInteger(c_headerSize + i * c_bytesInLong, c_bytesInLong, false, TypeDescriptorKind.integerUnsigned);

            emitFixedLength(word, c_bitsInLong);
        }

        if (remainingBits != 0)
        {
            long lastWord = other.m_currentWord >> (c_bitsInLong - remainingBits);

            emitFixedLength(lastWord, remainingBits);
        }
    }

    public void emitFixedLength(long v,
                                int length)
    {
        if (length > c_bitsInLong)
        {
            throw Exceptions.newIllegalArgumentException("Length greater than 64: %d", length);
        }

        //
        // Data is written in big-endian format, MSB first, LSB last.
        //
        int currentOffset        = m_lengthInBits % c_bitsInLong;
        int availableInFirstHalf = (c_bitsInLong - currentOffset);
        int firstHalfLength      = Math.min(length, availableInFirstHalf);
        int secondHalfSpill      = length - firstHalfLength;

        //
        // 1) Insert the first half.
        //
        long valueLeftPadded = v << (c_bitsInLong - length);
        m_currentWord |= (valueLeftPadded >>> currentOffset);

        if (currentOffset + firstHalfLength == c_bitsInLong)
        {
            int offset = m_data.size();

            m_data.grow(c_bytesInLong);
            m_data.setGenericInteger(offset, m_currentWord, c_bytesInLong, false);
            m_currentWord = 0;
        }

        //
        // 2) Insert the second half, if present.
        //
        if (secondHalfSpill != 0)
        {
            long secondHalfLeftPadded = v << (c_bitsInLong - secondHalfSpill);
            m_currentWord = secondHalfLeftPadded;
        }

        m_lengthInBits += length;
    }

    public int computeUnsignedVariableLength(long v)
    {
        int numberOfSignificantBits = c_bitsInLong - Long.numberOfLeadingZeros(v);

        switch (numberOfSignificantBits)
        {
            case 0:
            case 1:
            case 2:
            case 3:
                return 2 + 3;

            case 4:
            case 5:
                return 2 + 5;

            case 6:
            case 7:
                return 3 + 7;

            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
                return 3 + 12;

            default:
                //
                // Encoding for 13 significant bits = 0
                // Encoding for 64 significant bits = 51
                //
                int biasedNumberOfSignificantBits = numberOfSignificantBits - 13;
                int variableLength = (biasedNumberOfSignificantBits + 3) / 4;

                return 2 + 4 + (variableLength * 4 + 13);
        }
    }

    public void emitUnsignedVariableLength(long v)
    {
        int numberOfSignificantBits = c_bitsInLong - Long.numberOfLeadingZeros(v);

        switch (numberOfSignificantBits)
        {
            case 0:
            case 1:
            case 2:
            case 3:
                emitFixedLength(BitBufferEncoding.VariableLength3, 2);
                emitFixedLength(v, 3);
                break;

            case 4:
            case 5:
                emitFixedLength(BitBufferEncoding.VariableLength5, 2);
                emitFixedLength(v, 5);
                break;

            case 6:
            case 7:
                emitFixedLength(BitBufferEncoding.VariableLength7, 3);
                emitFixedLength(v, 7);
                break;

            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
                emitFixedLength(BitBufferEncoding.VariableLength12, 3);
                emitFixedLength(v, 12);
                break;

            default:
                //
                // Encoding for 13 significant bits = 0
                // Encoding for 64 significant bits = 51
                //
                int biasedNumberOfSignificantBits = numberOfSignificantBits - 13;
                int variableLength = (biasedNumberOfSignificantBits + 3) / 4;

                emitFixedLength(BitBufferEncoding.VariableLengthN, 2);
                emitFixedLength(variableLength, 4);

                //
                // For legacy reasons, we encode some large numbers with more than 64 bits, due to rounding.
                // 'emitFixedLength()' does not handle more than 64 bits of length.
                // So we have to push an extra bit, just for backward compatibility.
                //
                int length = variableLength * 4 + 13;
                if (length > 64)
                {
                    emitFixedLength(v < 0 ? -1 : 0, length - 64);
                    length = 64;
                }
                emitFixedLength(v, length);
                break;
        }
    }

    public int computeSignedVariableLength(long v)
    {
        return 1 + computeUnsignedVariableLength(Math.abs(v));
    }

    public void emitSignedVariableLength(long v)
    {
        boolean negative = v < 0;

        emitFixedLength(negative ? 1 : 0, 1);
        emitUnsignedVariableLength(Math.abs(v));
    }

    public void emitDouble(double d)
    {
        emitFixedLength(Double.doubleToRawLongBits(d), 64);
    }

    public void emitFloat(float f)
    {
        emitFixedLength(Float.floatToRawIntBits(f), 32);
    }

    public void emitBitSet(BitSet v)
    {
        int len = v.length();

        emitUnsignedVariableLength(len);

        for (int idx = 0; idx < len; idx++)
        {
            emitFixedLength(v.get(idx) ? 1 : 0, 1);
        }
    }

    public void emitByteArray(byte[] buffer)
    {
        emitByteArray(buffer, 0, buffer.length);
    }

    public void emitByteArray(byte[] buffer,
                              int offset,
                              int length)
    {
        if (offset < 0 || (offset + length > buffer.length))
        {
            throw new IndexOutOfBoundsException();
        }

        emitUnsignedVariableLength(length);

        for (int i = 0; i < length; i++)
        {
            emitFixedLength(buffer[offset + i], 8);
        }
    }

    public void emitString(String val)
    {
        emitByteArray(val.getBytes(Charsets.UTF_8));
    }

    //--//

    public void reset()
    {
        m_lengthInBits = 0;
        m_currentWord  = 0;
        m_data.clear();

        m_data.growTo(c_headerSize);
        m_data.setGenericInteger(0, 0, c_headerSize, false); // Make room for header.
    }

    public int sizeInBits()
    {
        return m_lengthInBits;
    }

    public int sizeInBytes()
    {
        int lengthInBytes = (m_lengthInBits + 7) / 8;

        return c_headerSize + lengthInBytes;
    }

    public void write(OutputStream stream) throws
                                           IOException
    {
        flush();

        m_data.toStream(stream, 0, m_data.size());
    }

    private byte[] createOutputBuffer(int minSize)
    {
        flush();

        byte[] buf = new byte[Math.max(minSize, 4 + ((m_lengthInBits + 7) / 8))];

        m_data.toArray(buf);

        return buf;
    }

    private void flush()
    {
        //
        // All data is written as big-endian.
        //
        m_data.setGenericInteger(0, m_lengthInBits, c_headerSize, false);

        int lengthInBytes = sizeInBytes();
        m_data.growTo(lengthInBytes);

        int leftoverInBits = m_lengthInBits % c_bitsInLong;
        if (leftoverInBits > 0)
        {
            int leftoverInBytes = (leftoverInBits + 7) / 8;

            m_data.setGenericInteger(lengthInBytes - leftoverInBytes, m_currentWord >> (c_bitsInLong - leftoverInBytes * 8), leftoverInBytes, false);
        }
    }

    public byte[] toByteArray()
    {
        return toByteArray(0);
    }

    public byte[] toByteArray(int minSize)
    {
        return createOutputBuffer(minSize);
    }

    public InputBuffer toInputBuffer()
    {
        flush();

        return InputBuffer.createFrom(m_data);
    }

    public OutputBuffer compress()
    {
        flush();

        ExpandableArrayOfBytes compressed = ExpandableArrayOfBytes.create();

        m_data.compressTo(compressed);

        return new OutputBuffer(compressed);
    }
}
