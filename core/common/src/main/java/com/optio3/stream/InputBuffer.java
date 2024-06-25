/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.stream;

import java.nio.charset.Charset;
import java.util.BitSet;

import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.serialization.TypeDescriptorKind;

public final class InputBuffer implements AutoCloseable
{
    public static final Logger LoggerInstance = new Logger(InputBuffer.class);

    //--//

    public boolean littleEndian;

    private final boolean                m_dumpContext;
    private       ExpandableArrayOfBytes m_data;
    private       int                    m_cursor;

    private InputBuffer(ExpandableArrayOfBytes data)
    {
        m_data   = data;
        m_cursor = 0;

        m_dumpContext = LoggerInstance.isEnabled(Severity.Debug);
    }

    public InputBuffer(OutputBuffer data)
    {
        this(ExpandableArrayOfBytes.create());

        data.copyTo(m_data, 0);
    }

    @Override
    public void close()
    {
        if (m_data != null)
        {
            m_data.close();
            m_data = null;
        }
    }

    public static InputBuffer createFrom(byte[] data)
    {
        return new InputBuffer(ExpandableArrayOfBytes.create(data));
    }

    public static InputBuffer createFrom(byte[] array,
                                         int offset,
                                         int count)
    {
        return new InputBuffer(ExpandableArrayOfBytes.create(array, offset, count));
    }

    public static InputBuffer createFrom(ExpandableArrayOfBytes data)
    {
        return takeOwnership(data.copy());
    }

    public static InputBuffer takeOwnership(ExpandableArrayOfBytes data)
    {
        return new InputBuffer(data);
    }

    public byte peekNextByte()
    {
        int pos = validateLengthAndAdvance(1);

        // Rewind back the cursor.
        m_cursor--;

        return m_data.get(pos, (byte) 0);
    }

    public int read1ByteUnsigned()
    {
        int pos = validateLengthAndAdvance(1);

        return m_data.get(pos, (byte) 0) & 0xFF;
    }

    public int read2BytesUnsigned()
    {
        return (int) readGenericInteger(2, TypeDescriptorKind.integerUnsigned);
    }

    public int read3BytesUnsigned()
    {
        return (int) readGenericInteger(3, TypeDescriptorKind.integerUnsigned);
    }

    public long read4BytesUnsigned()
    {
        return (int) readGenericInteger(4, TypeDescriptorKind.integerUnsigned);
    }

    public int read1ByteSigned()
    {
        return (byte) read1ByteUnsigned();
    }

    public int read2BytesSigned()
    {
        return (int) readGenericInteger(2, TypeDescriptorKind.integerSigned);
    }

    public int read3BytesSigned()
    {
        return (int) readGenericInteger(3, TypeDescriptorKind.integerSigned);
    }

    public int read4BytesSigned()
    {
        return (int) readGenericInteger(4, TypeDescriptorKind.integerSigned);
    }

    public long read8BytesSigned()
    {
        return readGenericInteger(8, TypeDescriptorKind.integerSigned);
    }

    public long readGenericInteger(int length,
                                   TypeDescriptorKind kind)
    {
        int pos = validateLengthAndAdvance(length);

        return m_data.getGenericInteger(pos, length, littleEndian, kind);
    }

    public double readDouble()
    {
        return Double.longBitsToDouble(read8BytesSigned());
    }

    public float readFloat()
    {
        return Float.intBitsToFloat(read4BytesSigned());
    }

    public BitSet readU1ByteAsBitSet()
    {
        BitSet bs = new BitSet(8);

        int v = read1ByteUnsigned();

        for (int idx = 0; idx < 8; idx++)
        {
            if ((v & (1 << idx)) != 0)
            {
                bs.set(idx);
            }
        }

        return bs;
    }

    public byte[] readByteArray(int length)
    {
        byte[] buffer = new byte[length];

        read(buffer, 0, length);

        return buffer;
    }

    public void read(byte[] buffer,
                     int offset,
                     int length)
    {
        int from = validateLengthAndAdvance(length);

        m_data.toArray(from, buffer, offset, length);
    }

    public InputBuffer readNestedBlock(int length)
    {
        int from = validateLengthAndAdvance(length);

        ExpandableArrayOfBytes sub = ExpandableArrayOfBytes.create();

        m_data.copyTo(from, length, sub, 0);

        return new InputBuffer(sub);
    }

    public String readBytesAsString(int length,
                                    Charset charset)
    {
        if (charset == null)
        {
            charset = Charset.defaultCharset();
        }

        if (length < 0)
        {
            length = remainingLength();
        }

        int from = validateLengthAndAdvance(length);

        byte[] buf = ExpandableArrayOfBytes.getTempBuffer(length);
        m_data.toArray(from, buf, 0, length);

        return new String(buf, 0, length, charset);
    }

    //--//

    public boolean isEOF()
    {
        return m_cursor == size();
    }

    public int getPosition()
    {
        return m_cursor;
    }

    public void setPosition(int pos)
    {
        if (pos < 0 || pos > size())
        {
            throw new IndexOutOfBoundsException();
        }

        m_cursor = pos;
    }

    public int size()
    {
        return m_data.size();
    }

    public int remainingLength()
    {
        return size() - getPosition();
    }

    public byte[] toArray()
    {
        return m_data.toArray();
    }

    public void copyTo(int srcOffset,
                       int srcLength,
                       OutputBuffer ob)
    {
        if (srcOffset < 0 || (srcOffset + srcLength > size()))
        {
            throw new IndexOutOfBoundsException();
        }

        ob.copyFrom(m_data, srcOffset, srcLength);
    }

    //--//

    private int validateLengthAndAdvance(int length)
    {
        int oldCursor = getPosition();

        setPosition(oldCursor + length);

        if (m_dumpContext)
        {
            dumpContext(oldCursor, length);
        }

        return oldCursor;
    }

    private void dumpContext(int pos,
                             int length)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("IN Offset:%d ", pos));

        for (int before = -2; before < 0; before++)
        {
            dumpByteIfPresent(sb, pos + before);
        }

        sb.append(">> ");

        for (int i = 0; i < length; i++)
        {
            dumpByteIfPresent(sb, pos + i);
        }

        sb.append("<< ");

        for (int after = 0; after < 2; after++)
        {
            dumpByteIfPresent(sb, pos + length + after);
        }

        LoggerInstance.debug(sb.toString());
    }

    private void dumpByteIfPresent(StringBuilder sb,
                                   int i)
    {
        if (i >= 0 && i < size())
        {
            sb.append(String.format("%02X ", m_data.get(i, (byte) 0)));
        }
    }
}
