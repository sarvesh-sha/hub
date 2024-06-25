/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import com.optio3.util.function.PredicateWithException;
import org.kamranzafar.jtar.TarEntry;
import org.kamranzafar.jtar.TarInputStream;

public class TarWalker implements AutoCloseable
{
    private static class TarInputStreamWithFlush extends TarInputStream
    {
        TarInputStreamWithFlush(InputStream in)
        {
            super(in);
        }

        void flushEntry() throws
                          IOException
        {
            closeCurrentEntry();
        }
    }

    private final TarInputStreamWithFlush m_tar;

    public TarWalker(InputStream stream,
                     boolean decompress) throws
                                         IOException
    {
        if (decompress)
        {
            stream = new GZIPInputStream(stream);
        }

        m_tar = new TarInputStreamWithFlush(stream);
    }

    @Override
    public void close() throws
                        IOException
    {
        m_tar.close();
    }

    //--//

    public static boolean walk(File file,
                               boolean decompress,
                               PredicateWithException<TarArchiveEntry> callback) throws
                                                                                 Exception
    {
        try (FileInputStream stream = new FileInputStream(file))
        {
            return walk(stream, decompress, callback);
        }
    }

    public static boolean walk(InputStream stream,
                               boolean decompress,
                               PredicateWithException<TarArchiveEntry> callback) throws
                                                                                 Exception
    {
        try (TarWalker walker = new TarWalker(stream, decompress))
        {
            return walker.walk(callback);
        }
    }

    public boolean walk(PredicateWithException<TarArchiveEntry> callback) throws
                                                                          Exception
    {
        while (true)
        {
            long     offsetStart = m_tar.getCurrentOffset();
            TarEntry entry       = m_tar.getNextEntry();
            if (entry == null)
            {
                break;
            }
            long offsetEnd = m_tar.getCurrentOffset();

            if (!callback.test(new TarArchiveEntry(m_tar, entry, offsetStart, offsetEnd)))
            {
                return false;
            }

            m_tar.flushEntry();
        }

        return true;
    }
}
