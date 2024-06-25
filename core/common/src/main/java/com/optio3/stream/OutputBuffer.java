/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.BitSet;

import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;

public final class OutputBuffer implements AutoCloseable
{
    public static final Logger LoggerInstance = new Logger(OutputBuffer.class);

    //--//

    private final boolean m_dumpContext;

    public boolean littleEndian;

    private ExpandableArrayOfBytes m_data;
    private int                    m_offset;

    public OutputBuffer()
    {
        this(ExpandableArrayOfBytes.create());
    }

    OutputBuffer(ExpandableArrayOfBytes data)
    {
        m_data        = data;
        m_dumpContext = LoggerInstance.isEnabled(Severity.Debug);
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

    public void emit1Byte(int v)
    {
        int offset = ensureData(1);

        m_data.set(offset, (byte) v);

        if (m_dumpContext)
        {
            dumpContext(1);
        }
    }

    public void emit2Bytes(int v)
    {
        emitGenericInteger(v, 2);
    }

    public void emit3Bytes(int v)
    {
        emitGenericInteger(v, 3);
    }

    public void emit4Bytes(int v)
    {
        emitGenericInteger(v, 4);
    }

    public void emit8Bytes(long v)
    {
        emitGenericInteger(v, 8);
    }

    public void emitGenericInteger(long v,
                                   int length)
    {
        int pos = ensureData(length);

        m_data.setGenericInteger(pos, v, length, littleEndian);

        if (m_dumpContext)
        {
            dumpContext(length);
        }
    }

    public void emit(double d)
    {
        emit8Bytes(Double.doubleToRawLongBits(d));
    }

    public void emit(float f)
    {
        emit4Bytes(Float.floatToRawIntBits(f));
    }

    public void emitAs1Byte(BitSet v)
    {
        int res = 0;

        for (int idx = 0; idx < 8; idx++)
        {
            if (v.get(idx))
            {
                res |= 1 << idx;
            }
        }

        emit1Byte(res);
    }

    public void emit(byte[] buffer)
    {
        emit(buffer, 0, buffer.length);
    }

    public void emit(byte[] buffer,
                     int offset,
                     int length)
    {
        int dstOffset = ensureData(length);

        m_data.setRange(dstOffset, buffer, offset, length);

        if (m_dumpContext)
        {
            dumpContext(length);
        }
    }

    public void emitNestedBlock(OutputBuffer v)
    {
        emitNestedBlock(v, 0, v.size());
    }

    public void emitNestedBlock(OutputBuffer v,
                                int offset,
                                int length)
    {
        if (offset < 0 || (offset + length > v.size()))
        {
            throw new IndexOutOfBoundsException();
        }

        v.m_data.copyTo(offset, length, m_data, m_offset);

        m_offset += length;
    }

    public void emitStringAsBytes(String val,
                                  Charset charset)
    {
        if (charset == null)
        {
            charset = Charset.defaultCharset();
        }

        emit(val.getBytes(charset));
    }

    //--//

    public void reset()
    {
        m_data.clear();
        m_offset = 0;
    }

    public int getPosition()
    {
        return m_offset;
    }

    public void setPosition(int pos)
    {
        if (pos < 0 || pos > m_data.size())
        {
            throw new IndexOutOfBoundsException();
        }

        m_offset = pos;
    }

    public int size()
    {
        return m_data.size();
    }

    public byte[] toByteArray()
    {
        return toByteArray(0);
    }

    public byte[] toByteArray(int minSize)
    {
        byte[] res = new byte[Math.max(minSize, m_data.size())];
        toByteArray(0, res, 0, res.length);
        return res;
    }

    public void toByteArray(int srcOffset,
                            byte[] buf,
                            int dstOffset,
                            int length)
    {
        if (srcOffset < 0 || length < 0 || dstOffset + length > buf.length)
        {
            throw new IndexOutOfBoundsException();
        }

        m_data.toArray(srcOffset, buf, dstOffset, length);
    }

    public void toStream(OutputStream stream,
                         int offset,
                         int length) throws
                                     IOException
    {
        if (offset < 0 || length < 0 || offset + length > m_data.size())
        {
            throw new IndexOutOfBoundsException();
        }

        m_data.toStream(stream, offset, length);
    }

    public void copyTo(ExpandableArrayOfBytes dst,
                       int dstOffset)
    {
        m_data.copyTo(0, size(), dst, dstOffset);
    }

    public void copyFrom(ExpandableArrayOfBytes src,
                         int srcOffset,
                         int srcLength)
    {
        src.copyTo(srcOffset, srcLength, m_data, size());
    }

    //--//

    private int ensureData(int length)
    {
        int offset = m_offset;

        int currentSize = m_data.size();
        int neededSize  = offset + length;
        if (neededSize > currentSize)
        {
            m_data.grow(neededSize - currentSize);
        }

        m_offset += length;

        return offset;
    }

    //--//

    private void dumpContext(int length)
    {
        int pos = m_offset - length;

        StringBuilder sb = new StringBuilder();

        sb.append(String.format("OUT Offset:%d ", pos));

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
        if (i >= 0 && i < m_data.size())
        {
            sb.append(String.format("%02X ", m_data.get(i, (byte) 0)));
        }
    }
}
