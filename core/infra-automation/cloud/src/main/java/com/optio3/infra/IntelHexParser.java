/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.BufferUtils;
import com.optio3.util.Exceptions;
import org.apache.commons.io.IOUtils;

public class IntelHexParser
{
    public static class Chunk
    {
        public final long   rangeStart;
        public final long   rangeEnd;
        public final byte[] data;

        public Chunk(long rangeStart,
                     long rangeEnd,
                     byte[] data)
        {
            this.rangeStart = rangeStart;
            this.rangeEnd   = rangeEnd;
            this.data       = data;
        }

        public static Chunk mergeIfOverlapping(Chunk before,
                                               Chunk after)
        {
            if (before.rangeStart > after.rangeEnd || after.rangeStart > before.rangeEnd)
            {
                // Non-overlapping.
                return null;
            }

            try (OutputBuffer ob = new OutputBuffer())
            {
                if (before.rangeStart < after.rangeStart)
                {
                    ob.emit(before.data);
                    ob.setPosition((int) (after.rangeStart - before.rangeStart));
                    ob.emit(after.data);
                }
                else
                {
                    ob.emit(after.data);
                    ob.setPosition((int) (before.rangeStart - after.rangeStart));
                    ob.emit(before.data);
                    ob.setPosition(0);
                    ob.emit(after.data);
                }

                return new Chunk(Math.min(before.rangeStart, after.rangeStart), Math.max(before.rangeEnd, after.rangeEnd), ob.toByteArray());
            }
        }
    }

    //--//

    public List<Chunk> chunks = Lists.newArrayList();

    //--//

    public IntelHexParser(InputStream stream) throws
                                              IOException
    {
        long addressBase = 0;

        for (String line : IOUtils.readLines(stream, Charset.defaultCharset()))
        {
            InputBuffer ib = parseLine(line);
            if (ib != null)
            {
                ib.littleEndian = false;

                //
                // A record (line of text) consists of six fields (parts) that appear in order from left to right:[7]
                //
                //  * Start code, one character, an ASCII colon ':'.
                //  * Byte count, two hex digits (one hex digit pair), indicating the number of bytes (hex digit pairs) in the data field. The maximum byte count is 255 (0xFF). 16 (0x10) and 32 (0x20) are commonly used byte counts.
                //  * Address, four hex digits, representing the 16-bit beginning memory address offset of the data.
                //      The physical address of the data is computed by adding this offset to a previously established base address, thus allowing memory addressing beyond the 64 kilobyte limit of 16-bit addresses.
                //      The base address, which defaults to zero, can be changed by various types of records. Base addresses and address offsets are always expressed as big endian values.
                //  * Record type (see record types below), two hex digits, 00 to 05, defining the meaning of the data field.
                //  * Data, a sequence of n bytes of data, represented by 2n hex digits. Some records omit this field (n equals zero). The meaning and interpretation of data bytes depends on the application.
                //  * Checksum, two hex digits, a computed value that can be used to verify the record has no errors.
                //

                int count        = ib.read1ByteUnsigned();
                int expectedSize = 1 + 2 + 1 + count + 1;
                if (ib.size() < expectedSize)
                {
                    throw Exceptions.newIllegalArgumentException("Line '%s' is invalid (too short)", line);
                }

                int    address  = ib.read2BytesUnsigned();
                int    type     = ib.read1ByteUnsigned();
                byte[] data     = ib.readByteArray(count);
                int    checksum = ib.read1ByteUnsigned();

                ib.setPosition(0);
                int checksumActual = 0;
                for (int i = 0; i < ib.size() - 1; i++)
                {
                    checksumActual += ib.read1ByteUnsigned();
                }

                checksumActual = (-checksumActual) & 0xFF;
                if (checksum != checksumActual)
                {
                    throw Exceptions.newIllegalArgumentException("Line '%s' is invalid (checksum mismatch: %02x != %02x)", line, checksum, checksumActual);
                }

                switch (type)
                {
                    case 0: // Data
                        long chunkAddress = addressBase + address;

                        Chunk chunk = new Chunk(chunkAddress, chunkAddress + data.length, data);
                        chunks.add(chunk);
                        break;

                    case 1: // End Of File;
                        return;

                    case 2: // Extended Segment Address
                        throw Exceptions.newIllegalArgumentException("Line '%s' is unsupported: Extended Segment Address", line);

                    case 3: // Start Segment Address
                        throw Exceptions.newIllegalArgumentException("Line '%s' is unsupported: Start Segment Address", line);

                    case 4: // Extended Linear Address
                        ib.setPosition(4);
                        addressBase = ib.read2BytesUnsigned() << 16;
                        break;

                    case 5: // Start Linear Address
                        ib.setPosition(4);
                        addressBase = ib.read4BytesUnsigned();
                        break;
                }
            }
        }
    }

    public void compact()
    {
        List<Chunk> newChunks = Lists.newArrayList();

        for (Chunk chunk : chunks)
        {
            Chunk chunkRunning = chunk;

            Iterator<Chunk> it = newChunks.iterator();
            while (it.hasNext())
            {
                Chunk merged = Chunk.mergeIfOverlapping(it.next(), chunkRunning);
                if (merged != null)
                {
                    it.remove();
                    chunkRunning = merged;
                }
            }

            newChunks.add(chunkRunning);
        }

        chunks.clear();
        chunks.addAll(newChunks);
    }

    private static InputBuffer parseLine(String line)
    {
        if (line.startsWith(":"))
        {
            byte[] buf = BufferUtils.convertFromHex(line.substring(1));
            if (buf.length > 0)
            {
                return InputBuffer.createFrom(buf);
            }
        }

        return null;
    }
}
