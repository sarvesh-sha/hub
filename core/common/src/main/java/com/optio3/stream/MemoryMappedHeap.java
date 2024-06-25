/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.stream;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ConcurrentModificationException;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.util.FileSystem;

/**
 * A growable heap that is backed by a set of memory-mapped files.<br>
 *
 * It exposes two APIs, one block-based, the other stream-based.<br>
 *
 * The block-based API either returns the absolute position of the allocated chunk, or a ByteBuffer that can be used to access it.<br>
 *
 * The stream-based API can span segments and provides allocating output streams, which will grow the heap, or fixed-sized input/output streams.
 * The allocating output stream has extra methods to get the absolute position of the stream, plus ability to move in the stream and read back data.
 */
public class MemoryMappedHeap implements AutoCloseable
{
    public class SeekableInputOutputStream extends OutputStream
    {
        private final long m_offset;
        private final long m_length;
        private       long m_cursor;

        private SeekableInputOutputStream(long offset,
                                          long length)
        {
            m_offset = offset;
            m_length = length;
        }

        //--//

        public long absolutePosition()
        {
            return m_offset;
        }

        public long position()
        {
            return m_cursor;
        }

        public void position(long position)
        {
            if (position < 0 || position > m_length)
            {
                throw new IndexOutOfBoundsException();
            }

            m_cursor = position;
        }

        public long available()
        {
            return m_length - m_cursor;
        }

        //--//

        public int read() throws
                          IOException
        {
            if (m_cursor >= m_length)
            {
                return -1;
            }

            int val = MemoryMappedHeap.this.read(m_offset + m_cursor);
            m_cursor++;
            return val;
        }

        public int read(byte[] b,
                        int off,
                        int len) throws
                                 IOException
        {
            if (len == 0)
            {
                return 0;
            }

            len = (int) Math.min(len, available());

            int read = MemoryMappedHeap.this.read(m_offset + m_cursor, b, off, len);
            if (read == 0)
            {
                return -1;
            }

            m_cursor += read;
            return read;
        }

        //--//

        @Override
        public void write(int b) throws
                                 IOException
        {
            if (m_cursor >= m_length)
            {
                throw new IndexOutOfBoundsException();
            }

            MemoryMappedHeap.this.write(m_offset + m_cursor, (byte) b);

            m_cursor++;
        }

        @Override
        public void write(byte[] b,
                          int off,
                          int len) throws
                                   IOException
        {
            while (len > 0)
            {
                long position = m_offset + m_cursor;

                int segmentNum    = (int) (position >> m_segmentShift);
                int segmentOffset = (int) (position & m_segmentMask);

                int available = (int) Math.min(Math.min(len, m_length - m_cursor), m_segmentSize - segmentOffset);

                synchronized (MemoryMappedHeap.this)
                {
                    ByteBuffer byteBuffer = getBuffer(segmentNum);

                    byteBuffer.position(segmentOffset);
                    byteBuffer.put(b, off, available);
                }

                off += available;
                len -= available;
                m_cursor += available;
            }
        }
    }

    public class SkippableOutputStream extends OutputStream
    {
        private final long m_offset;
        private       long m_cursor = 0;

        private SkippableOutputStream(long offset)
        {
            m_offset = offset;
        }

        @Override
        public void close()
        {
            if (m_cursor < 0)
            {
                return; // Already closed.
            }

            synchronized (MemoryMappedHeap.this)
            {
                m_pendingGrowth = null;
            }
        }

        public long absolutePosition()
        {
            return m_offset;
        }

        public long position()
        {
            return m_cursor;
        }

        //--//

        public void skip(long length) throws
                                      IOException
        {
            while (length > 0)
            {
                long position = m_offset + m_cursor;

                int segmentOffset = (int) (position & m_segmentMask);

                int available = (int) Math.min(length, m_segmentSize - segmentOffset);

                long positionWanted = position + available;
                long needed         = positionWanted - lengthInner();
                if (needed > 0)
                {
                    allocateContiguousChunkInner(needed);
                }

                length -= available;
                m_cursor += available;
            }
        }

        //--//

        @Override
        public void write(int b) throws
                                 IOException
        {
            long pos = m_offset + m_cursor;

            if (pos == lengthInner())
            {
                allocateContiguousChunkInner(1);
            }

            int segmentNum    = (int) (pos >> m_segmentShift);
            int segmentOffset = (int) (pos & m_segmentMask);

            ByteBuffer byteBuffer = getBuffer(segmentNum);

            byteBuffer.put(segmentOffset, (byte) b);

            m_cursor++;
        }

        @Override
        public void write(byte[] b,
                          int off,
                          int len) throws
                                   IOException
        {
            while (len > 0)
            {
                long position = m_offset + m_cursor;

                int segmentNum    = (int) (position >> m_segmentShift);
                int segmentOffset = (int) (position & m_segmentMask);

                int available = (int) Math.min(len, m_segmentSize - segmentOffset);

                long positionWanted = position + available;
                long needed         = positionWanted - lengthInner();
                if (needed > 0)
                {
                    allocateContiguousChunkInner(needed);
                }

                synchronized (MemoryMappedHeap.this)
                {
                    ByteBuffer byteBuffer = getBuffer(segmentNum);

                    byteBuffer.position(segmentOffset);
                    byteBuffer.put(b, off, available);
                }

                off += available;
                len -= available;
                m_cursor += available;
            }
        }
    }

    private class InputStreamImpl extends InputStream
    {
        private final long m_offset;
        private final long m_length;
        private       long m_cursor;

        private InputStreamImpl(long offset,
                                long length)
        {
            m_offset = offset;
            m_length = length;
        }

        //--//

        @Override
        public int available()
        {
            return (int) (m_length - m_cursor);
        }

        @Override
        public void reset()
        {
            m_cursor = 0;
        }

        @Override
        public long skip(long n)
        {
            n = Math.min(n, available());

            m_cursor += n;

            return n;
        }

        @Override
        public int read() throws
                          IOException
        {
            if (m_cursor >= m_length)
            {
                return -1;
            }

            int val = MemoryMappedHeap.this.read(m_offset + m_cursor);
            m_cursor++;
            return val;
        }

        @Override
        public int read(byte[] b,
                        int off,
                        int len) throws
                                 IOException
        {
            if (len == 0)
            {
                return 0;
            }

            len = (int) Math.min(len, available());

            int read = MemoryMappedHeap.this.read(m_offset + m_cursor, b, off, len);
            if (read == 0)
            {
                return -1;
            }

            m_cursor += read;
            return read;
        }
    }

    //--//

    private class Segment
    {
        private final RandomAccessFile          m_file;
        private final ByteBuffer                m_bufferInMem;
        private       SoftReference<ByteBuffer> m_bufferOnDisk;

        Segment(long segmentSize,
                File file) throws
                           IOException
        {
            if (file != null)
            {
                m_file = new RandomAccessFile(file, "rw");
                m_file.setLength(segmentSize);
                m_bufferInMem = null;
            }
            else
            {
                m_file        = null;
                m_bufferInMem = ByteBuffer.allocate((int) m_segmentSize);
            }
        }

        void close() throws
                     IOException
        {
            if (m_file != null)
            {
                m_file.close();
            }
        }

        ByteBuffer ensureBufferOpen() throws
                                      IOException
        {
            if (m_bufferInMem != null)
            {
                return m_bufferInMem;
            }

            ByteBuffer buffer = m_bufferOnDisk != null ? m_bufferOnDisk.get() : null;

            if (buffer == null)
            {
                buffer = m_file.getChannel()
                               .map(FileChannel.MapMode.READ_WRITE, 0, m_segmentSize);

                m_bufferOnDisk = new SoftReference<>(buffer);
            }

            return buffer;
        }
    }

    //--//

    private final Path          m_rootDir;
    private final long          m_spillToDiskThreshold;
    private final long          m_segmentSize;
    private final long          m_segmentShift;
    private final long          m_segmentMask;
    private final List<Segment> m_segments = Lists.newArrayList();

    private long                  m_totalLength;
    private long                  m_availableInActiveSegment;
    private int                   m_sequenceNumber;
    private SkippableOutputStream m_pendingGrowth;

    public MemoryMappedHeap(String tmpPrefix,
                            long segmentSize,
                            long inMemorySegments)
    {
        int left  = Long.numberOfLeadingZeros(segmentSize);
        int right = Long.numberOfTrailingZeros(segmentSize);

        if (left + right == 63)
        {
            m_segmentShift = right;
        }
        else
        {
            m_segmentShift = 64 - left;
        }

        try
        {
            if (tmpPrefix == null)
            {
                m_rootDir = Files.createTempDirectory("MemoryMappedHeap");
            }
            else
            {
                m_rootDir = FileSystem.resolveTempDirectory(tmpPrefix);

                FileSystem.deleteDirectory(m_rootDir);
                FileSystem.createDirectory(m_rootDir);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        m_segmentSize = 1 << m_segmentShift;
        m_segmentMask = m_segmentSize - 1;

        m_spillToDiskThreshold = m_segmentSize * inMemorySegments;
    }

    @Override
    public void close() throws
                        Exception
    {
        for (Segment segment : m_segments)
        {
            segment.close();
        }
        m_segments.clear();

        FileSystem.deleteDirectory(m_rootDir);

        m_totalLength              = 0;
        m_availableInActiveSegment = 0;
        m_sequenceNumber           = 0;
        m_pendingGrowth            = null;
    }

    //--//

    public static void serializeString(DataOutputStream stream,
                                       String value) throws
                                                     IOException
    {
        if (value == null)
        {
            stream.writeInt(-1);
        }
        else
        {
            byte[] buf = value.getBytes(StandardCharsets.UTF_8);
            stream.writeInt(buf.length);
            stream.write(buf);
        }
    }

    public static String deserializeString(DataInputStream stream) throws
                                                                   IOException
    {
        String contents;

        int contentsLength = stream.readInt();
        if (contentsLength < 0)
        {
            return null;
        }

        byte[] buf = new byte[contentsLength];
        if (stream.read(buf) != contentsLength)
        {
            throw new IOException("deserializeString: EOF");
        }

        return new String(buf, StandardCharsets.UTF_8);
    }

    //--//

    public long segmentSize()
    {
        return m_segmentSize;
    }

    public synchronized long length()
    {
        return lengthInner();
    }

    public synchronized long allocateContigousChunk(long size) throws
                                                               IOException
    {
        if (m_pendingGrowth != null)
        {
            throw new ConcurrentModificationException("MemoryMappedHeap already in use by output stream");
        }

        return allocateContiguousChunkInner(size);
    }

    public synchronized SkippableOutputStream allocateAsOutputStream()
    {
        if (m_pendingGrowth != null)
        {
            throw new ConcurrentModificationException("MemoryMappedHeap already in use by output stream");
        }

        m_pendingGrowth = new SkippableOutputStream(length());
        return m_pendingGrowth;
    }

    public synchronized SeekableInputOutputStream allocateAsFixedSizeOutputStream(long length) throws
                                                                                               IOException
    {
        long offset = length();

        try (SkippableOutputStream stream = allocateAsOutputStream())
        {
            stream.skip(length);
        }

        return sliceAsOutputStream(offset, length);
    }

    public ByteBuffer slice(long offset,
                            long count) throws
                                        IOException
    {
        int segmentNum    = (int) (offset >> m_segmentShift);
        int segmentOffset = (int) (offset & m_segmentMask);

        if (segmentOffset + count > m_segmentSize)
        {
            throw new IndexOutOfBoundsException("Slice overlaps segment boundary");
        }

        ByteBuffer slice;

        synchronized (this)
        {
            slice = getBuffer(segmentNum).slice();
        }

        slice.position(segmentOffset);
        slice.limit(segmentOffset + (int) count);
        return slice;
    }

    public synchronized InputStream sliceAsInputStream(long offset,
                                                       long length)
    {
        if (m_pendingGrowth != null)
        {
            throw new ConcurrentModificationException("MemoryMappedHeap already in use by output stream");
        }

        if (offset < 0 || length < 0)
        {
            throw new IndexOutOfBoundsException();
        }

        if (offset + length > length())
        {
            throw new IndexOutOfBoundsException();
        }

        return new InputStreamImpl(offset, length);
    }

    public synchronized SeekableInputOutputStream sliceAsOutputStream(long offset,
                                                                      long length)
    {
        if (m_pendingGrowth != null)
        {
            throw new ConcurrentModificationException("MemoryMappedHeap already in use by output stream");
        }

        if (offset < 0 || length < 0)
        {
            throw new IndexOutOfBoundsException();
        }

        if (offset + length > length())
        {
            throw new IndexOutOfBoundsException();
        }

        return new SeekableInputOutputStream(offset, length);
    }

    //--//

    private long lengthInner()
    {
        return m_totalLength;
    }

    private long allocateContiguousChunkInner(long size) throws
                                                         IOException
    {
        if (size < 0)
        {
            throw new IndexOutOfBoundsException("Allocation size is negative: " + size);
        }

        if (size > m_segmentSize)
        {
            throw new IndexOutOfBoundsException("Allocation request bigger than maximum segment size: " + m_segmentSize);
        }

        if (m_availableInActiveSegment < size) // If too big for the current segment, allocate a new segment.
        {
            m_totalLength += m_availableInActiveSegment;

            Segment newSegment;

            if (m_totalLength >= m_spillToDiskThreshold)
            {
                String name = String.format("chunk.%08d.tmp", m_sequenceNumber++);
                Path   file = m_rootDir.resolve(name);

                newSegment = new Segment(m_segmentSize, file.toFile());
            }
            else
            {
                newSegment = new Segment(m_segmentSize, null);
            }

            m_segments.add(newSegment);

            m_availableInActiveSegment = m_segmentSize;
        }

        long pos = m_totalLength;

        m_availableInActiveSegment -= size;
        m_totalLength += size;

        return pos;
    }

    //--//

    private int read(long position) throws
                                    IOException
    {
        int segmentNum    = (int) (position >> m_segmentShift);
        int segmentOffset = (int) (position & m_segmentMask);

        synchronized (MemoryMappedHeap.this)
        {
            ByteBuffer byteBuffer = getBuffer(segmentNum);

            return byteBuffer.get(segmentOffset) & 0xFF;
        }
    }

    private int read(long position,
                     byte[] buffer,
                     int offset,
                     int len) throws
                              IOException
    {
        int read = 0;

        while (len > 0)
        {
            int segmentNum       = (int) (position >> m_segmentShift);
            int segmentOffset    = (int) (position & m_segmentMask);
            int segmentAvailable = (int) (m_segmentSize - segmentOffset);
            int available        = Math.min(len, segmentAvailable);

            synchronized (MemoryMappedHeap.this)
            {
                ByteBuffer byteBuffer = getBuffer(segmentNum);

                byteBuffer.position(segmentOffset);
                byteBuffer.get(buffer, offset, available);
            }

            position += available;
            offset += available;
            len -= available;
            read += available;
        }

        return read;
    }

    private void write(long pos,
                       byte b) throws
                               IOException
    {
        if (pos < 0 || pos >= length())
        {
            throw new IndexOutOfBoundsException();
        }

        int segmentNum    = (int) (pos >> m_segmentShift);
        int segmentOffset = (int) (pos & m_segmentMask);

        ByteBuffer byteBuffer = getBuffer(segmentNum);

        byteBuffer.put(segmentOffset, b);
    }

    //--//

    private ByteBuffer getBuffer(int segmentNum) throws
                                                 IOException
    {
        return m_segments.get(segmentNum)
                         .ensureBufferOpen();
    }
}