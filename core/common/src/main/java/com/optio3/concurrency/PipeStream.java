/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.concurrency;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class PipeStream implements AutoCloseable
{
    public class Writer extends OutputStream
    {
        @Override
        public void write(int b) throws
                                 IOException
        {
            byte[] buf = new byte[1];

            buf[0] = (byte) b;

            write(buf);
        }

        @Override
        public void write(byte[] b,
                          int off,
                          int len)
        {
            synchronized (m_data)
            {
                if (m_closed)
                {
                    return;
                }

                b = Arrays.copyOfRange(b, off, off + len);
                m_data.add(b);
                m_data.notifyAll();
            }
        }
    }

    public class Reader extends InputStream
    {
        private byte[] m_currentBuffer;
        private int    m_offset;

        @Override
        public int read()
        {
            byte[] buf = new byte[1];

            int len = read(buf, 0, 1);

            return len < 0 ? -1 : buf[0] & 0xFF;
        }

        @Override
        public int read(byte[] b,
                        int off,
                        int len)
        {
            int got = 0;

            while (len > 0)
            {
                if (m_currentBuffer != null)
                {
                    int available = m_currentBuffer.length - m_offset;
                    if (available > 0)
                    {
                        int toCopy = Math.min(available, len);

                        System.arraycopy(m_currentBuffer, m_offset, b, off, toCopy);
                        off += toCopy;
                        len -= toCopy;
                        got += toCopy;
                        m_offset += toCopy;
                    }
                    else
                    {
                        m_currentBuffer = null;
                    }
                }
                else
                {
                    synchronized (m_data)
                    {
                        if (m_closed)
                        {
                            return got > 0 ? got : -1;
                        }

                        m_offset = 0;
                        m_currentBuffer = m_data.poll();
                        if (m_currentBuffer == null)
                        {
                            //
                            // No more data in the queue, but we got some, so return.
                            //
                            if (got > 0)
                            {
                                return got;
                            }

                            try
                            {
                                m_data.wait();
                            }
                            catch (InterruptedException e)
                            {
                            }
                        }
                    }
                }
            }

            return got;
        }
    }

    private final Queue<byte[]> m_data = new LinkedList<>();
    private       boolean       m_closed;

    private final OutputStream m_output = new Writer();
    private final InputStream  m_input  = new Reader();

    @Override
    public void close()
    {
        synchronized (m_data)
        {
            m_closed = true;
            m_data.notifyAll();
        }
    }

    public OutputStream getOutputStream()
    {
        return m_output;
    }

    public InputStream getInputStream()
    {
        return m_input;
    }
}
