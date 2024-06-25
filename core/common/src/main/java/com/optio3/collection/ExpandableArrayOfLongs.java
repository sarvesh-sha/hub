/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.collection;

import java.util.Arrays;

import com.optio3.util.BoxingUtils;
import com.optio3.util.ObjectRecycler;

public class ExpandableArrayOfLongs implements AutoCloseable
{
    private static final int c_elementSizeLog2   = 3;
    private static final int c_elementSize       = 1 << c_elementSizeLog2;
    private static final int c_segmentSizeInBits = (12 - c_elementSizeLog2); // Make segments 4096 bytes long.
    private static final int c_segmentSize       = 1 << c_segmentSizeInBits;

    private final static ObjectRecycler<State>  s_stateRecycler;
    private final static ObjectRecycler<long[]> s_segmentRecycler;

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
        s_segmentRecycler = ObjectRecycler.build((int) (segmentCacheSize / (c_segmentSize * c_elementSize)), 64, long[].class, () -> new long[c_segmentSize], null);
    }

    static class State
    {
        long[][] segmentedArrays = new long[10][];

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
        public final long[] tempBuffer;

        Batch()
        {
            tempBuffer = s_segmentRecycler.acquireRaw();
        }

        @Override
        public void close()
        {
            s_segmentRecycler.releaseRaw(tempBuffer);
        }

        public void fill(long value)
        {
            Arrays.fill(tempBuffer, value);
        }

        public void getRange(int arrayOffset,
                             int batchOffset,
                             int length)
        {
            ExpandableArrayOfLongs.this.getRange(arrayOffset, tempBuffer, batchOffset, length);
        }

        public void setRange(int arrayOffset,
                             int batchOffset,
                             int length)
        {
            ExpandableArrayOfLongs.this.setRange(arrayOffset, tempBuffer, batchOffset, length);
        }

        public void addRange(int batchOffset,
                             int length)
        {
            ExpandableArrayOfLongs.this.addRange(tempBuffer, batchOffset, length);
        }
    }

    //--//

    private final State m_state;
    private       int   m_size;

    //--//

    private ExpandableArrayOfLongs(State state)
    {
        m_state = state;
    }

    public static ExpandableArrayOfLongs create()
    {
        return new ExpandableArrayOfLongs(s_stateRecycler.acquireRaw());
    }

    public static ExpandableArrayOfLongs create(long[] array)
    {
        return create(array, 0, array.length);
    }

    public static ExpandableArrayOfLongs create(long[] array,
                                                int offset,
                                                int count)
    {
        var res = create();
        res.addRange(array, offset, count);
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

    public int binarySearch(long key)
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

    public void fromArray(long[] array)
    {
        clear();

        addRange(array, 0, array.length);
    }

    public void addRange(long[] array,
                         int offset,
                         int count)
    {
        int position = m_size;

        grow(count);

        setRange(position, array, offset, count);
    }

    public void add(long value)
    {
        ensureSlotForNewValues(1);

        int index  = computeIndex(m_size);
        int offset = computeOffset(m_size);

        m_state.segmentedArrays[index][offset] = value;
        m_size++;
    }

    public long insert(int pos,
                       long value)
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

            long[] segmentedArray = m_state.segmentedArrays[chunkIndex];

            // Save the last value, it will be overwritten.
            long lastValue = segmentedArray[chunkEndOffset];

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

    public long remove(int pos,
                       boolean rotate)
    {
        if (pos < 0 || pos >= m_size)
        {
            return 0;
        }

        long value = rotate ? get(pos, 0) : 0;

        int lowerLimitPos = pos;

        pos = --m_size; // Pos points to the last slot we need to copy.

        while (true)
        {
            int chunkIndex     = computeIndex(pos);
            int chunkEndOffset = computeOffset(pos);

            boolean crossSegment     = chunkIndex != computeIndex(lowerLimitPos);
            int     chunkStartOffset = crossSegment ? 0 : computeOffset(lowerLimitPos);

            int slotsToMove = chunkEndOffset - chunkStartOffset;

            long[] segmentedArray = m_state.segmentedArrays[chunkIndex];

            // Save the first value, it will be overwritten.
            long firstValue = segmentedArray[chunkStartOffset];

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

    public long get(int position,
                    long defaultValue)
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
                         long[] values,
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
                       long value)
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
                         long[] values,
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

    public ExpandableArrayOfLongs copy()
    {
        var copy = ExpandableArrayOfLongs.create();

        copyTo(0, m_size, copy, 0);

        return copy;
    }

    public void copyTo(int srcOffset,
                       int srcLength,
                       ExpandableArrayOfLongs dst,
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

    public long[] toArray()
    {
        var result = new long[m_size];
        toArray(result);
        return result;
    }

    public void toArray(long[] buf)
    {
        toArray(0, buf, 0, buf.length);
    }

    public void toArray(int srcOffset,
                        long[] buf,
                        int dstOffset,
                        int length)
    {
        length = Math.min(length, m_size - srcOffset);

        long[][] segmentedArrays = m_state.segmentedArrays;

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
