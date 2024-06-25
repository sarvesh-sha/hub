/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.archive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.util.function.PredicateWithException;
import org.apache.commons.io.IOUtils;

public class ZipWalker implements AutoCloseable
{
    //
    // Wrap a PushbackInputStream to keep track of actual stream position.
    //
    private static class PushbackInputStreamWithPosition extends PushbackInputStream
    {
        private long m_currentOffset;

        PushbackInputStreamWithPosition(InputStream in)
        {
            super(in, 512);
        }

        @Override
        public int read() throws
                          IOException
        {
            int result = super.read();
            if (result >= 0)
            {
                m_currentOffset++;
            }

            return result;
        }

        @Override
        public int read(byte[] b,
                        int off,
                        int len) throws
                                 IOException
        {
            int result = super.read(b, off, len);
            if (result >= 0)
            {
                m_currentOffset += result;
            }

            return result;
        }

        @Override
        public void unread(int b) throws
                                  IOException
        {
            super.unread(b);

            m_currentOffset--;
        }

        @Override
        public void unread(byte[] b,
                           int off,
                           int len) throws
                                    IOException
        {
            super.unread(b, off, len);

            m_currentOffset -= len;
        }

        @Override
        public void unread(byte[] b) throws
                                     IOException
        {
            super.unread(b);

            m_currentOffset -= b.length;
        }

        @Override
        public long skip(long n) throws
                                 IOException
        {
            long result = super.skip(n);
            if (result >= 0)
            {
                m_currentOffset += result;
            }

            return result;
        }

        long getCurrentOffset()
        {
            return m_currentOffset;
        }
    }

    //
    // Wrap a ZipInputStream so we can substitute the inner stream with one that keeps track of logical position.
    //
    private static class ZipInputStreamWithPosition extends ZipInputStream
    {
        ZipInputStreamWithPosition(InputStream in)
        {
            super(in);

            this.in = new PushbackInputStreamWithPosition(in);
        }

        long getCurrentOffset()
        {
            return ((PushbackInputStreamWithPosition) this.in).getCurrentOffset();
        }
    }

    public class ArchiveEntry
    {
        private final ZipEntry    m_entry;
        private final long        m_entryOffset;
        private       long        m_entryLength;
        private       String      m_name;
        private       InputStream m_stream;
        private       byte[]      m_contents;

        ArchiveEntry(ZipEntry entry,
                     long entryOffset)
        {
            m_entry = (ZipEntry) entry.clone();
            m_entryOffset = entryOffset;
        }

        public boolean isDirectory()
        {
            return m_entry.isDirectory();
        }

        public String getName()
        {
            if (m_name == null)
            {
                String name = m_entry.getName();
                if (name.startsWith("./"))
                {
                    name = name.substring(2);
                }

                m_name = name;
            }

            return m_name;
        }

        public void loadInMemory() throws
                                   IOException
        {
            if (m_contents == null)
            {
                if (m_stream != null)
                {
                    throw new RuntimeException("Stream already open");
                }

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                IOUtils.copyLarge(m_zip, output);
                m_contents = output.toByteArray();
            }
        }

        public long getFileOffset()
        {
            return m_entryOffset;
        }

        public long getFileSize()
        {
            return m_entryLength;
        }

        public long getSize()
        {
            return m_entry.getSize();
        }

        public void setTime(ZonedDateTime dt)
        {
            m_entry.setTime(dt.toEpochSecond() * 1000);
        }

        public LocalDateTime getLastModifiedTime()
        {
            return LocalDateTime.ofInstant(m_entry.getLastModifiedTime()
                                                  .toInstant(), ZoneId.systemDefault());
        }

        public InputStream getStream()
        {
            if (m_stream == null)
            {
                if (m_contents != null)
                {
                    m_stream = new ByteArrayInputStream(m_contents);
                }
                else
                {
                    m_stream = new InputStream()
                    {
                        @Override
                        public int read() throws
                                          IOException
                        {
                            return m_zip.read();
                        }

                        @Override
                        public int read(byte[] b,
                                        int off,
                                        int len) throws
                                                 IOException
                        {
                            return m_zip.read(b, off, len);
                        }
                    };
                }
            }

            return m_stream;
        }
    }

    private final ZipInputStreamWithPosition m_zip;

    public ZipWalker(InputStream stream)
    {
        m_zip = new ZipInputStreamWithPosition(stream);
    }

    @Override
    public void close() throws
                        IOException
    {
        m_zip.close();
    }

    //--//

    public static Map<String, Long> computeSizes(File file) throws
                                                            Exception
    {
        try (FileInputStream stream = new FileInputStream(file))
        {
            return computeSizes(stream);
        }
    }

    public static Map<String, Long> computeSizes(InputStream stream) throws
                                                                     Exception
    {
        Map<String, Long> sizes = Maps.newHashMap();
        byte[]            buf   = new byte[1024];

        walk(stream, (entry) ->
        {
            InputStream zipFileStream = entry.getStream();
            long        len           = 0;
            int         read;

            while ((read = zipFileStream.read(buf)) > 0)
            {
                len += read;
            }

            sizes.put(entry.getName(), len);
            return true;
        });

        return sizes;
    }

    public static boolean walk(File file,
                               PredicateWithException<ArchiveEntry> callback) throws
                                                                              Exception
    {
        try (FileInputStream stream = new FileInputStream(file))
        {
            return walk(stream, callback);
        }
    }

    public static boolean walk(InputStream stream,
                               PredicateWithException<ArchiveEntry> callback) throws
                                                                              Exception
    {
        try (ZipWalker walker = new ZipWalker(stream))
        {
            return walker.walk(callback);
        }
    }

    public boolean walk(PredicateWithException<ArchiveEntry> callback) throws
                                                                       Exception
    {
        while (true)
        {
            long pos = m_zip.getCurrentOffset();

            ZipEntry entry = m_zip.getNextEntry();
            if (entry == null)
            {
                break;
            }

            ArchiveEntry archiveEntry = new ArchiveEntry(entry, pos);
            if (!callback.test(archiveEntry))
            {
                return false;
            }

            // We need to close the entry to get the correct end offset.
            m_zip.closeEntry();

            archiveEntry.m_entryLength = m_zip.getCurrentOffset() - archiveEntry.m_entryOffset;
        }

        return true;
    }

    //--//

    public static List<ArchiveEntry> load(InputStream input) throws
                                                             Exception
    {
        List<ArchiveEntry> entries = Lists.newArrayList();

        ZipWalker.walk(input, (entry) ->
        {
            entry.loadInMemory();
            entries.add(entry);
            return true;
        });

        return entries;
    }

    public static void save(OutputStream output,
                            int compressionLevel,
                            List<ArchiveEntry> entries) throws
                                                        IOException
    {
        try (ZipOutputStream zip = new ZipOutputStream(output))
        {
            zip.setLevel(compressionLevel);

            for (ArchiveEntry inMemoryEntry : entries)
            {
                // Reset the compressed size, since we might have changed the compression level
                inMemoryEntry.m_entry.setCompressedSize(-1);

                zip.putNextEntry(inMemoryEntry.m_entry);
                zip.write(inMemoryEntry.m_contents);
                zip.closeEntry();
            }
        }
    }
}