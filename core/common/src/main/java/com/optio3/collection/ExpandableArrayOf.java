/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.collection;

import java.lang.reflect.Array;
import java.util.Arrays;

import com.optio3.util.BoxingUtils;
import com.optio3.util.ObjectRecycler;

public abstract class ExpandableArrayOf<T> implements AutoCloseable
{
    private static final int c_elementSizeLog2   = 3; // Assume 64-bit pointers.
    private static final int c_elementSize       = 1 << c_elementSizeLog2;
    private static final int c_segmentSizeInBits = (12 - c_elementSizeLog2); // Make segments 4096 bytes long.
    private static final int c_segmentSize       = 1 << c_segmentSizeInBits;

    private final static ObjectRecycler<State>    s_stateRecycler;
    private final static ObjectRecycler<Object[]> s_segmentRecycler;

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
        s_segmentRecycler = ObjectRecycler.build((int) (segmentCacheSize / (c_segmentSize * c_elementSize)), 32, Object[].class, () -> new Object[c_segmentSize], (array) -> Arrays.fill(array, null));
    }

    static class State
    {
        Object[][] segmentedArrays = new Object[10][];

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

    //--//

    @FunctionalInterface
    public interface BinarySearchComparer<T>
    {
        int compare(T key);
    }

    //--//

    private final Class<T> m_clz;
    private final State    m_state;
    private       int      m_size;

    //--//

    protected ExpandableArrayOf(Class<T> clz)
    {
        m_clz   = clz;
        m_state = s_stateRecycler.acquireRaw();
    }

    protected abstract ExpandableArrayOf<T> allocate();

    protected abstract int compare(T o1,
                                   T o2);

    @Override
    public final void close()
    {
        m_state.release();
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

    public int binarySearch(T key)
    {
        int low  = 0;
        int high = m_size - 1;

        while (low <= high)
        {
            int mid    = (low + high) >>> 1;
            T   midVal = getTypedSegment(computeIndex(mid))[computeOffset(mid)];

            int cmp = compare(midVal, key);
            if (cmp < 0)
            {
                low = mid + 1;
            }
            else if (cmp > 0)
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

    public int binarySearch(BinarySearchComparer<T> comparer)
    {
        int low  = 0;
        int high = m_size - 1;

        while (low <= high)
        {
            int mid    = (low + high) >>> 1;
            T   midVal = getTypedSegment(computeIndex(mid))[computeOffset(mid)];

            int cmp = comparer.compare(midVal);
            if (cmp < 0)
            {
                low = mid + 1;
            }
            else if (cmp > 0)
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

    public void fromArray(T[] array)
    {
        clear();

        addRange(array, 0, array.length);
    }

    public void addRange(T[] array,
                         int offset,
                         int count)
    {
        int position = m_size;

        grow(count);

        setRange(position, array, offset, count);
    }

    public void add(T value)
    {
        ensureSlotForNewValues(1);

        int index  = computeIndex(m_size);
        int offset = computeOffset(m_size);

        getTypedSegment(index)[offset] = value;
        m_size++;
    }

    public T insert(int pos,
                    T value)
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

            T[] segmentedArray = getTypedSegment(chunkIndex);

            // Save the last value, it will be overwritten.
            T lastValue = segmentedArray[chunkEndOffset];

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

    public T remove(int pos,
                    boolean rotate)
    {
        if (pos < 0 || pos >= m_size)
        {
            return null;
        }

        T value = rotate ? get(pos, null) : null;

        int lowerLimitPos = pos;

        pos = --m_size; // Pos points to the last slot we need to copy.

        while (true)
        {
            int chunkIndex     = computeIndex(pos);
            int chunkEndOffset = computeOffset(pos);

            boolean crossSegment     = chunkIndex != computeIndex(lowerLimitPos);
            int     chunkStartOffset = crossSegment ? 0 : computeOffset(lowerLimitPos);

            int slotsToMove = chunkEndOffset - chunkStartOffset;

            T[] segmentedArray = getTypedSegment(chunkIndex);

            // Save the first value, it will be overwritten.
            T firstValue = segmentedArray[chunkStartOffset];

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

    public T get(int position,
                 T defaultValue)
    {
        if (position < 0 || position >= m_size)
        {
            return defaultValue;
        }

        int index  = computeIndex(position);
        int offset = computeOffset(position);

        return getTypedSegment(index)[offset];
    }

    public boolean set(int position,
                       T value)
    {
        if (position < 0 || position >= m_size)
        {
            return false;
        }

        int index  = computeIndex(position);
        int offset = computeOffset(position);

        getTypedSegment(index)[offset] = value;
        return true;
    }

    public boolean setRange(int position,
                            T[] values,
                            int offset,
                            int length)
    {
        if (position < 0 || position + length > m_size)
        {
            return false;
        }

        while (length > 0)
        {
            int segmentIndex  = computeIndex(position);
            int segmentOffset = computeOffset(position);
            int segmentLength = Math.min(c_segmentSize - segmentOffset, length);

            System.arraycopy(values, offset, getTypedSegment(segmentIndex), segmentOffset, segmentLength);

            position += segmentLength;
            offset += segmentLength;
            length -= segmentLength;
        }

        return true;
    }

    //--//

    public ExpandableArrayOf<T> copy()
    {
        var copy = allocate();

        copyTo(0, m_size, copy, 0);

        return copy;
    }

    public void copyTo(int srcOffset,
                       int srcLength,
                       ExpandableArrayOf<T> dst,
                       int dstOffset)
    {
        dst.growTo(dstOffset + srcLength);

        while (srcLength > 0)
        {
            int segmentIndex  = computeIndex(srcOffset);
            int segmentOffset = computeOffset(srcOffset);
            int segmentLength = Math.min(c_segmentSize - segmentOffset, srcLength);

            dst.setRange(dstOffset, getTypedSegment(segmentIndex), segmentOffset, segmentLength);

            dstOffset += segmentLength;
            srcOffset += segmentLength;
            srcLength -= segmentLength;
        }
    }

    public T[] toArray()
    {
        Object arrayUntyped = Array.newInstance(m_clz, m_size);

        @SuppressWarnings("unchecked") T[] array = (T[]) arrayUntyped;

        toArray(array);
        return array;
    }

    public void toArray(T[] buf)
    {
        toArray(0, buf, 0, buf.length);
    }

    public void toArray(int srcOffset,
                        T[] buf,
                        int dstOffset,
                        int length)
    {
        length = Math.min(length, m_size - srcOffset);

        while (length > 0)
        {
            int segmentIndex  = computeIndex(srcOffset);
            int segmentOffset = computeOffset(srcOffset);
            int segmentLength = Math.min(c_segmentSize - segmentOffset, length);

            System.arraycopy(getTypedSegment(segmentIndex), segmentOffset, buf, dstOffset, segmentLength);

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

    @SuppressWarnings("unchecked")
    private T[] getTypedSegment(int index)
    {
        return (T[]) m_state.segmentedArrays[index];
    }
}
