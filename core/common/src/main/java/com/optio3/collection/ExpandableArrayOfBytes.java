/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.collection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.optio3.serialization.TypeDescriptorKind;
import com.optio3.util.BoxingUtils;
import com.optio3.util.ObjectRecycler;

public class ExpandableArrayOfBytes implements AutoCloseable
{
    private static final int c_elementSizeLog2   = 0;
    private static final int c_elementSize       = 1 << c_elementSizeLog2;
    private static final int c_segmentSizeInBits = (12 - c_elementSizeLog2); // Make segments 4096 bytes long.
    private static final int c_segmentSize       = 1 << c_segmentSizeInBits;

    private final static ObjectRecycler<State>  s_stateRecycler;
    private final static ObjectRecycler<byte[]> s_segmentRecycler;

    static
    {
        int  hostCacheSize;
        long segmentCacheSize;

        if (ObjectRecycler.isARM)
        {
            hostCacheSize    = 128;
            segmentCacheSize = 4 * 1024 * 1024;
        }
        else
        {
            hostCacheSize    = 1024;
            segmentCacheSize = 16 * 1024 * 1024;
        }

        s_stateRecycler   = ObjectRecycler.build(hostCacheSize, 64, State.class, State::new, null);
        s_segmentRecycler = ObjectRecycler.build((int) (segmentCacheSize / (c_segmentSize * c_elementSize)), 32, byte[].class, () -> new byte[c_segmentSize], null);
    }

    private final static ThreadLocal<SoftReference<byte[]>> s_localBuffer = new ThreadLocal<>();

    public static byte[] getTempBuffer(int length)
    {
        var    ref = s_localBuffer.get();
        byte[] res = ref != null ? ref.get() : null;

        if (res == null || res.length < length)
        {
            res = new byte[length];
            s_localBuffer.set(new SoftReference<>(res));
        }

        return res;
    }

    static class State
    {
        byte[][] segmentedArrays = new byte[10][];

        void ensureSegments(int numSegments)
        {
            //
            // First expand array of arrays.
            //
            if (segmentedArrays.length < numSegments)
            {
                segmentedArrays = Arrays.copyOf(segmentedArrays, numSegments);
            }

            //
            // Then allocate all the missing arrays.
            //
            while (--numSegments >= 0 && segmentedArrays[numSegments] == null)
            {
                segmentedArrays[numSegments] = s_segmentRecycler.acquireRaw();
            }
        }

        void release()
        {
            clear();

            s_stateRecycler.releaseRaw(this);
        }

        void clear()
        {
            ExpandableArrayHelper.clear(s_segmentRecycler, segmentedArrays);
        }
    }

    public class Batch implements AutoCloseable
    {
        public final byte[] tempBuffer;

        Batch()
        {
            tempBuffer = s_segmentRecycler.acquireRaw();
        }

        @Override
        public void close()
        {
            s_segmentRecycler.releaseRaw(tempBuffer);
        }

        public void fill(byte value)
        {
            Arrays.fill(tempBuffer, value);
        }

        public void getRange(int arrayOffset,
                             int batchOffset,
                             int length)
        {
            ExpandableArrayOfBytes.this.getRange(arrayOffset, tempBuffer, batchOffset, length);
        }

        public void setRange(int arrayOffset,
                             int batchOffset,
                             int length)
        {
            ExpandableArrayOfBytes.this.setRange(arrayOffset, tempBuffer, batchOffset, length);
        }

        public void addRange(int batchOffset,
                             int length)
        {
            ExpandableArrayOfBytes.this.addRange(tempBuffer, batchOffset, length);
        }
    }

    //--//

    private final State m_state;
    private       int   m_size;

    //--//

    private ExpandableArrayOfBytes(State state)
    {
        m_state = state;
    }

    public static ExpandableArrayOfBytes create()
    {
        return new ExpandableArrayOfBytes(s_stateRecycler.acquireRaw());
    }

    public static ExpandableArrayOfBytes create(byte[] array)
    {
        return create(array, 0, array.length);
    }

    public static ExpandableArrayOfBytes create(byte[] array,
                                                int offset,
                                                int count)
    {
        var res = create();
        res.addRange(array, offset, count);
        return res;
    }

    public static ExpandableArrayOfBytes create(InputStream stream,
                                                int length) throws
                                                            IOException
    {
        ExpandableArrayOfBytes res = ExpandableArrayOfBytes.create();

        res.fromStream(stream, length);

        return res;
    }

    @Override
    public void close()
    {
        m_state.release();
    }

    public Batch prepareBatch()
    {
        return new Batch();
    }

    public void growTo(int newSize)
    {
        if (newSize > m_size)
        {
            grow(newSize - m_size);
        }
    }

    public void grow(int extraValues)
    {
        ensureSlotForNewValues(extraValues);

        m_size += extraValues;
    }

    public void prepareForGrowth(int extraValues)
    {
        ensureSlotForNewValues(extraValues);
    }

    public int size()
    {
        return m_size;
    }

    public int segmentSize()
    {
        return c_segmentSize;
    }

    public void clear()
    {
        m_size = 0;
    }

    public int binarySearch(byte key)
    {
        int low    = 0;
        int high   = m_size - 1;
        var arrays = m_state.segmentedArrays;

        while (low <= high)
        {
            int mid    = (low + high) >>> 1;
            var midVal = arrays[computeIndex(mid)][computeOffset(mid)];

            if (midVal < key)
            {
                low = mid + 1;
            }
            else if (midVal > key)
            {
                high = mid - 1;
            }
            else
            {
                return mid; // key found
            }
        }

        return -(low + 1);  // key not found.
    }

    public void fromArray(byte[] array)
    {
        clear();

        addRange(array, 0, array.length);
    }

    public void addRange(byte[] array,
                         int offset,
                         int count)
    {
        int position = m_size;

        grow(count);

        setRange(position, array, offset, count);
    }

    public void add(byte value)
    {
        ensureSlotForNewValues(1);

        int index  = computeIndex(m_size);
        int offset = computeOffset(m_size);

        m_state.segmentedArrays[index][offset] = value;
        m_size++;
    }

    public byte insert(int pos,
                       byte value)
    {
        pos = BoxingUtils.bound(pos, 0, m_size);

        int upperLimitPos = m_size;

        grow(1);

        while (true)
        {
            int chunkIndex       = computeIndex(pos);
            int chunkStartOffset = computeOffset(pos);

            boolean crossSegment   = chunkIndex != computeIndex(upperLimitPos);
            int     chunkEndOffset = crossSegment ? c_segmentSize - 1 : computeOffset(upperLimitPos);

            int slotsToMove = chunkEndOffset - chunkStartOffset;

            byte[] segmentedArray = m_state.segmentedArrays[chunkIndex];

            // Save the last value, it will be overwritten.
            byte lastValue = segmentedArray[chunkEndOffset];

            // Move everything to the right.
            System.arraycopy(segmentedArray, chunkStartOffset, segmentedArray, chunkStartOffset + 1, slotsToMove);

            // Insert value.
            segmentedArray[chunkStartOffset] = value;

            pos   = chunkIndex * c_segmentSize + chunkEndOffset + 1;
            value = lastValue;

            if (pos > upperLimitPos)
            {
                return value;
            }
        }
    }

    public byte remove(int pos,
                       boolean rotate)
    {
        if (pos < 0 || pos >= m_size)
        {
            return 0;
        }

        byte value = rotate ? get(pos, (byte) 0) : 0;

        int lowerLimitPos = pos;

        pos = --m_size; // Pos points to the last slot we need to copy.

        while (true)
        {
            int chunkIndex     = computeIndex(pos);
            int chunkEndOffset = computeOffset(pos);

            boolean crossSegment     = chunkIndex != computeIndex(lowerLimitPos);
            int     chunkStartOffset = crossSegment ? 0 : computeOffset(lowerLimitPos);

            int slotsToMove = chunkEndOffset - chunkStartOffset;

            byte[] segmentedArray = m_state.segmentedArrays[chunkIndex];

            // Save the first value, it will be overwritten.
            byte firstValue = segmentedArray[chunkStartOffset];

            // Move everything to the right.
            System.arraycopy(segmentedArray, chunkStartOffset + 1, segmentedArray, chunkStartOffset, slotsToMove);

            // Insert value.
            segmentedArray[chunkEndOffset] = value;

            pos   = chunkIndex * c_segmentSize + chunkStartOffset - 1;
            value = firstValue;

            if (pos < lowerLimitPos)
            {
                return value;
            }
        }
    }

    public byte get(int position,
                    byte defaultValue)
    {
        if (position < 0 || position >= m_size)
        {
            return defaultValue;
        }

        int index  = computeIndex(position);
        int offset = computeOffset(position);

        return m_state.segmentedArrays[index][offset];
    }

    public void getRange(int position,
                         byte[] values,
                         int offset,
                         int length)
    {
        if (position < 0 || position + length > m_size)
        {
            throw new ArrayIndexOutOfBoundsException();
        }

        while (length > 0)
        {
            int segmentIndex  = computeIndex(position);
            int segmentOffset = computeOffset(position);
            int segmentLength = Math.min(c_segmentSize - segmentOffset, length);

            System.arraycopy(m_state.segmentedArrays[segmentIndex], segmentOffset, values, offset, segmentLength);

            position += segmentLength;
            offset += segmentLength;
            length -= segmentLength;
        }
    }

    public boolean set(int position,
                       byte value)
    {
        if (position < 0 || position >= m_size)
        {
            return false;
        }

        int index  = computeIndex(position);
        int offset = computeOffset(position);

        m_state.segmentedArrays[index][offset] = value;
        return true;
    }

    public void setRange(int position,
                         byte[] values,
                         int offset,
                         int length)
    {
        if (position < 0 || position + length > m_size)
        {
            throw new ArrayIndexOutOfBoundsException();
        }

        while (length > 0)
        {
            int segmentIndex  = computeIndex(position);
            int segmentOffset = computeOffset(position);
            int segmentLength = Math.min(c_segmentSize - segmentOffset, length);

            System.arraycopy(values, offset, m_state.segmentedArrays[segmentIndex], segmentOffset, segmentLength);

            position += segmentLength;
            offset += segmentLength;
            length -= segmentLength;
        }
    }

    //--//

    public long getGenericInteger(int position,
                                  int length,
                                  boolean littleEndian,
                                  TypeDescriptorKind kind)
    {
        if (position < 0 || position + length > m_size)
        {
            return Long.MIN_VALUE;
        }

        int bitShift;
        int bitShiftDelta;

        if (littleEndian)
        {
            bitShift      = -8;
            bitShiftDelta = 8;
        }
        else
        {
            bitShift      = 8 * length;
            bitShiftDelta = -8;
        }

        int  rightShift = (8 - length) * 8;
        long res        = 0;

        while (length > 0)
        {
            int index     = computeIndex(position);
            int offset    = computeOffset(position);
            int lengthMax = Math.min(length, c_segmentSize - offset);

            byte[] data = m_state.segmentedArrays[index];

            // @formatter:off
            switch (lengthMax)
            {
                case 8: res |= (data[offset++] & 0xFFL) << (bitShift += bitShiftDelta);
                case 7: res |= (data[offset++] & 0xFFL) << (bitShift += bitShiftDelta);
                case 6: res |= (data[offset++] & 0xFFL) << (bitShift += bitShiftDelta);
                case 5: res |= (data[offset++] & 0xFFL) << (bitShift += bitShiftDelta);
                case 4: res |= (data[offset++] & 0xFFL) << (bitShift += bitShiftDelta);
                case 3: res |= (data[offset++] & 0xFFL) << (bitShift += bitShiftDelta);
                case 2: res |= (data[offset++] & 0xFFL) << (bitShift += bitShiftDelta);
                case 1: res |= (data[offset++] & 0xFFL) << (bitShift += bitShiftDelta);
            }
            // @formatter:on

            position += lengthMax;
            length -= lengthMax;
        }

        res <<= rightShift;

        //
        // Now 'res' is left-aligned.
        // To right-align, choose the correct shift.
        //
        return kind.signAwareRightShift(res, rightShift);
    }

    public boolean setGenericInteger(int position,
                                     long v,
                                     int length,
                                     boolean littleEndian)
    {
        if (position < 0 || position + length > m_size)
        {
            return false;
        }

        int bitShift;
        int bitShiftDelta;

        if (littleEndian)
        {
            bitShift      = -8;
            bitShiftDelta = 8;
        }
        else
        {
            bitShift      = 8 * length;
            bitShiftDelta = -8;
        }

        while (length > 0)
        {
            int index     = computeIndex(position);
            int offset    = computeOffset(position);
            int lengthMax = Math.min(length, c_segmentSize - offset);

            byte[] data = m_state.segmentedArrays[index];

            // @formatter:off
            switch (lengthMax)
            {
                case 8: data[offset++] = (byte) (v >> (bitShift += bitShiftDelta));
                case 7: data[offset++] = (byte) (v >> (bitShift += bitShiftDelta));
                case 6: data[offset++] = (byte) (v >> (bitShift += bitShiftDelta));
                case 5: data[offset++] = (byte) (v >> (bitShift += bitShiftDelta));
                case 4: data[offset++] = (byte) (v >> (bitShift += bitShiftDelta));
                case 3: data[offset++] = (byte) (v >> (bitShift += bitShiftDelta));
                case 2: data[offset++] = (byte) (v >> (bitShift += bitShiftDelta));
                case 1: data[offset++] = (byte) (v >> (bitShift += bitShiftDelta));
            }
            // @formatter:on

            position += lengthMax;
            length -= lengthMax;
        }

        return true;
    }

    //--//

    public ExpandableArrayOfBytes copy()
    {
        var copy = ExpandableArrayOfBytes.create();

        copyTo(0, m_size, copy, 0);

        return copy;
    }

    public void copyTo(int srcOffset,
                       int srcLength,
                       ExpandableArrayOfBytes dst,
                       int dstOffset)
    {
        dst.growTo(dstOffset + srcLength);

        while (srcLength > 0)
        {
            int segmentIndex  = computeIndex(srcOffset);
            int segmentOffset = computeOffset(srcOffset);
            int segmentLength = Math.min(c_segmentSize - segmentOffset, srcLength);

            dst.setRange(dstOffset, m_state.segmentedArrays[segmentIndex], segmentOffset, segmentLength);

            dstOffset += segmentLength;
            srcOffset += segmentLength;
            srcLength -= segmentLength;
        }
    }

    public byte[] toArray()
    {
        var result = new byte[m_size];
        toArray(result);
        return result;
    }

    public void toArray(byte[] buf)
    {
        toArray(0, buf, 0, buf.length);
    }

    public void toArray(int srcOffset,
                        byte[] buf,
                        int dstOffset,
                        int length)
    {
        length = Math.min(length, m_size - srcOffset);

        byte[][] segmentedArrays = m_state.segmentedArrays;

        while (length > 0)
        {
            int segmentIndex  = computeIndex(srcOffset);
            int segmentOffset = computeOffset(srcOffset);
            int segmentLength = Math.min(c_segmentSize - segmentOffset, length);

            System.arraycopy(segmentedArrays[segmentIndex], segmentOffset, buf, dstOffset, segmentLength);

            srcOffset += segmentLength;
            dstOffset += segmentLength;
            length -= segmentLength;
        }
    }

    //--//

    public void fromStream(InputStream stream,
                           int length) throws
                                       IOException
    {
        while (length != 0)
        {
            int segmentIndex  = computeIndex(m_size);
            int segmentOffset = computeOffset(m_size);
            int segmentLength = c_segmentSize - segmentOffset;

            if (length > 0)
            {
                segmentLength = Math.min(segmentLength, length);
            }

            prepareForGrowth(segmentLength);

            int read = stream.read(m_state.segmentedArrays[segmentIndex], segmentOffset, segmentLength);
            if (read < 0)
            {
                break;
            }

            m_size += read;

            if (length > 0)
            {
                length -= read;
            }
        }
    }

    public void toStream(OutputStream stream,
                         int offset,
                         int length) throws
                                     IOException
    {
        length = Math.min(length, m_size - offset);

        byte[][] segmentedArrays = m_state.segmentedArrays;

        while (length > 0)
        {
            int segmentIndex  = computeIndex(offset);
            int segmentOffset = computeOffset(offset);
            int segmentLength = Math.min(c_segmentSize - segmentOffset, length);

            stream.write(segmentedArrays[segmentIndex], segmentOffset, segmentLength);

            offset += segmentLength;
            length -= segmentLength;
        }
    }

    public void decompressTo(ExpandableArrayOfBytes output)
    {
        var inf = new Inflater();

        try
        {
            int offset = 0;
            int length = m_size;

            while (!inf.finished())
            {
                if (inf.needsInput())
                {
                    if (length == 0)
                    {
                        throw new RuntimeException("Unexpected end of ZLIB input stream");
                    }

                    int segmentIndex  = computeIndex(offset);
                    int segmentOffset = computeOffset(offset);
                    int segmentLength = Math.min(c_segmentSize - segmentOffset, length);

                    inf.setInput(m_state.segmentedArrays[segmentIndex], segmentOffset, segmentLength);

                    offset += segmentLength;
                    length -= segmentLength;
                }

                output.drainInflated(inf);
            }
        }
        catch (DataFormatException e)
        {
            String s = e.getMessage();
            throw new RuntimeException(s != null ? s : "Invalid ZLIB data format");
        }
        finally
        {
            inf.end();
        }
    }

    public void compressTo(ExpandableArrayOfBytes output)
    {
        var def = new Deflater();

        try
        {
            int srcOffset = 0;
            int srcLength = m_size;

            while (srcLength > 0)
            {
                int segmentIndex  = computeIndex(srcOffset);
                int segmentOffset = computeOffset(srcOffset);
                int segmentLength = Math.min(c_segmentSize - segmentOffset, srcLength);

                def.setInput(m_state.segmentedArrays[segmentIndex], segmentOffset, segmentLength);

                srcOffset += segmentLength;
                srcLength -= segmentLength;

                while (!def.needsInput())
                {
                    output.drainDeflated(def);
                }
            }

            def.finish();

            while (!def.finished())
            {
                output.drainDeflated(def);
            }
        }
        finally
        {
            def.end();
        }
    }

    private void drainInflated(Inflater inf) throws
                                             DataFormatException
    {
        prepareForGrowth(1);

        int segmentIndex  = computeIndex(m_size);
        int segmentOffset = computeOffset(m_size);
        int segmentLength = c_segmentSize - segmentOffset;

        int read = inf.inflate(m_state.segmentedArrays[segmentIndex], segmentOffset, segmentLength);
        m_size += read;
    }

    private void drainDeflated(Deflater def)
    {
        prepareForGrowth(512);

        int segmentIndex  = computeIndex(m_size);
        int segmentOffset = computeOffset(m_size);
        int segmentLength = c_segmentSize - segmentOffset;

        int written = def.deflate(m_state.segmentedArrays[segmentIndex], segmentOffset, segmentLength);
        m_size += written;
    }

    //--//

    private void ensureSlotForNewValues(int extraSize)
    {
        if (extraSize > 0)
        {
            int expectedSegments = computeIndex(m_size + extraSize + c_segmentSize - 1);

            m_state.ensureSegments(expectedSegments);
        }
    }

    private int computeIndex(int position)
    {
        return position >> c_segmentSizeInBits;
    }

    private int computeOffset(int position)
    {
        return position & (c_segmentSize - 1);
    }
}
