/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.stream;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ConcurrentModificationException;

import com.optio3.stream.MemoryMappedHeap;
import com.optio3.test.common.Optio3Test;
import org.junit.Test;

public class MemoryMappedHeapTest extends Optio3Test
{
    @Test
    public void basicTests() throws
                             Exception
    {
        try (MemoryMappedHeap heap = new MemoryMappedHeap("MemoryMappedHeapTest", 4000, 1))
        {
            assertEquals(4096, heap.segmentSize());

            assertEquals(0, heap.allocateContigousChunk(1024));
            assertEquals(1024, heap.allocateContigousChunk(1024));
            assertEquals(2048, heap.allocateContigousChunk(1024));
            assertEquals(3072, heap.allocateContigousChunk(1024));
            assertEquals(4096, heap.allocateContigousChunk(1024));
            assertEquals(4096 * 2, heap.allocateContigousChunk(4096));

            ByteBuffer buffer = heap.slice(0, 128);
            for (int i = 0; i < 128; i++)
            {
                buffer.put((byte) i);
            }

            ByteBuffer buffer2 = heap.slice(16, 128 - 16);
            for (int i = 0; i < 128 - 16; i++)
            {
                assertEquals(i + 16, buffer2.get());
            }

            InputStream stream = heap.sliceAsInputStream(0, 128);
            for (int i = 0; i < 128; i++)
            {
                assertEquals(i, stream.read());
            }

            assertEquals(-1, stream.read());

            stream = heap.sliceAsInputStream(16, 128 - 16);
            for (int i = 0; i < 128 - 16; i++)
            {
                assertEquals(i + 16, stream.read());
            }

            assertEquals(4096 * 3, heap.allocateContigousChunk(4096 - 1024 + 1));

            MemoryMappedHeap.SeekableInputOutputStream stream2 = heap.sliceAsOutputStream(4096 * 2 - 64, 128);
            assertEquals(128, stream2.available());

            for (int i = 0; i < 16; i++)
            {
                stream2.position(63);
                assertEquals(1 + 64, stream2.available());
                stream2.write(63 + i);
                assertEquals(64, stream2.available());
                stream2.write(64 + i);

                stream2.position(63);
                assertEquals(63 + i, stream2.read());
                assertEquals(64 + i, stream2.read());

                if (i % 4 == 0)
                {
                    System.gc();
                    System.runFinalization();
                }
            }
        }
    }

    @Test
    public void crossSegmentTests() throws
                                    Exception
    {
        try (MemoryMappedHeap heap = new MemoryMappedHeap("MemoryMappedHeapTest", 4000, 2))
        {
            try (MemoryMappedHeap.SkippableOutputStream outputStream = heap.allocateAsOutputStream())
            {
                outputStream.skip(8000);
            }

            MemoryMappedHeap.SeekableInputOutputStream stream2 = heap.sliceAsOutputStream(0, 128);
            assertEquals(128, stream2.available());

            for (int i = 0; i < 16; i++)
            {
                stream2.position(120);
                assertEquals(8, stream2.available());
                stream2.write(63 + i);
                stream2.position(120);
                assertEquals(63 + i, stream2.read());
            }

            stream2 = heap.sliceAsOutputStream(4000, 200);
            assertEquals(200, stream2.available());

            byte[] buf = new byte[100];
            for (int i = 0; i < buf.length; i++)
            {
                buf[i] = (byte) i;
            }

            byte[] buf2 = new byte[buf.length];

            for (int i = 0; i < 16; i++)
            {
                stream2.position(i);
                stream2.write(buf);

                stream2.position(i);
                assertEquals(100, stream2.read(buf2, 0, buf2.length));

                assertArrayEquals(buf, buf2);
            }
        }
    }

    @Test
    public void basicNegativeTests() throws
                                     Exception
    {
        try (MemoryMappedHeap heap = new MemoryMappedHeap("MemoryMappedHeapTest", 4096, 1))
        {
            assertFailure(IndexOutOfBoundsException.class, () -> heap.allocateContigousChunk(4097));

            heap.allocateContigousChunk(4096);
            heap.allocateContigousChunk(4096);

            assertFailure(IndexOutOfBoundsException.class, () -> heap.slice(4000, 128));

            OutputStream outputStream = heap.allocateAsOutputStream();

            assertFailure(ConcurrentModificationException.class, () -> heap.allocateContigousChunk(1024));
            assertFailure(ConcurrentModificationException.class, () -> heap.allocateAsOutputStream());

            outputStream.write(1);
            outputStream.close();

            assertEquals(8192 + 1, heap.length());

            outputStream = heap.allocateAsOutputStream();
            outputStream.write(2);
            outputStream.write(3);
            outputStream.write(4);
            outputStream.write(5);
            outputStream.close();
            assertEquals(8192 + 5, heap.length());

            InputStream stream = heap.sliceAsInputStream(8192, 5);
            for (int i = 0; i < 5; i++)
            {
                assertEquals(i + 1, stream.read());
            }
        }
    }
}
