/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileBackedInputStream extends InputStream implements AutoCloseable
{
    private FileSystem.TmpFileHolder m_tmpFileHolder;
    private FileInputStream          m_stream;

    public FileBackedInputStream(String prefix,
                                 String suffix) throws
                                                IOException
    {
        m_tmpFileHolder = FileSystem.createTempFile(prefix, suffix);
    }

    public File get()
    {
        return m_tmpFileHolder.get();
    }

    public FileOutputStream openForWrite() throws
                                           FileNotFoundException
    {
        return new FileOutputStream(get());
    }

    //--//

    @Override
    public void close() throws
                        IOException
    {
        if (m_stream != null)
        {
            m_stream.close();
            m_stream = null;
        }

        if (m_tmpFileHolder != null)
        {
            m_tmpFileHolder.close();
            m_tmpFileHolder = null;
        }
    }

    //--//

    @Override
    public int read() throws
                      IOException
    {
        return ensureStream().read();
    }

    @Override
    public int read(byte[] b) throws
                              IOException
    {
        return ensureStream().read(b);
    }

    @Override
    public int read(byte[] b,
                    int off,
                    int len) throws
                             IOException
    {
        return ensureStream().read(b, off, len);
    }

    @Override
    public long skip(long n) throws
                             IOException
    {
        return ensureStream().skip(n);
    }

    @Override
    public int available() throws
                           IOException
    {
        return ensureStream().available();
    }

    @Override
    public synchronized void reset() throws
                                     IOException
    {
        ensureStream().reset();
    }

    //--//

    private synchronized FileInputStream ensureStream() throws
                                                        FileNotFoundException
    {
        if (m_stream == null)
        {
            if (m_tmpFileHolder == null)
            {
                throw new FileNotFoundException();
            }

            m_stream = new FileInputStream(m_tmpFileHolder.get());
        }

        return m_stream;
    }
}
