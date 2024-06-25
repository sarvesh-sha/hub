/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.tags;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.BitSet;

import com.optio3.collection.ExpandableArrayOf;
import com.optio3.serialization.Reflection;
import com.optio3.stream.MemoryMappedHeap;

class OfflineBitSetArray
{
    private static class Offline
    {
        private final long                  m_position;
        private final long                  m_length;
        private       WeakReference<BitSet> m_weak;

        Offline(MemoryMappedHeap heap,
                BitSet bs) throws
                           IOException
        {
            try (MemoryMappedHeap.SkippableOutputStream stream = heap.allocateAsOutputStream())
            {
                DataOutputStream dataOutput = new DataOutputStream(stream);

                long[] values = bs.toLongArray();

                dataOutput.writeInt(values.length);

                for (int i = 0; i < values.length; i++)
                {
                    dataOutput.writeLong(values[i]);
                }

                m_position = stream.absolutePosition();
                m_length   = (int) stream.position();

                m_weak = new WeakReference<>(bs);
            }
        }

        BitSet get(MemoryMappedHeap heap)
        {
            if (m_weak != null)
            {
                BitSet bs = m_weak.get();
                if (bs != null)
                {
                    return bs;
                }
            }

            try
            {
                InputStream     stream    = heap.sliceAsInputStream(m_position, m_length);
                DataInputStream dataInput = new DataInputStream(stream);

                int    num    = dataInput.readInt();
                long[] values = new long[num];

                for (int i = 0; i < num; i++)
                {
                    values[i] = dataInput.readLong();
                }

                BitSet bs = BitSet.valueOf(values);
                m_weak = new WeakReference<>(bs);
                return bs;
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    static class BitsetArray extends ExpandableArrayOf<Object>
    {
        BitsetArray()
        {
            super(Object.class);
        }

        @Override
        protected ExpandableArrayOf<Object> allocate()
        {
            return new BitsetArray();
        }

        @Override
        protected int compare(Object o1,
                              Object o2)
        {
            throw new RuntimeException("Not supported");
        }
    }

    //--//

    private final MemoryMappedHeap m_heap;
    private final BitsetArray      m_array = new BitsetArray();

    OfflineBitSetArray(MemoryMappedHeap heap)
    {
        m_heap = heap;
    }

    int put(BitSet bs)
    {
        int index = m_array.size();

        m_array.add(bs);

        return index;
    }

    void update(int index,
                BitSet bs)
    {
        m_array.set(index, bs);
    }

    BitSet get(int index)
    {
        Object o = m_array.get(index, null);

        BitSet bs = Reflection.as(o, BitSet.class);
        if (bs != null)
        {
            return bs;
        }

        Offline offline = Reflection.as(o, Offline.class);
        if (offline != null)
        {
            return offline.get(m_heap);
        }

        return null;
    }

    //--//

    void ensureSerialized() throws
                            IOException
    {
        for (int i = 0; i < m_array.size(); i++)
        {
            Object o  = m_array.get(i, null);
            BitSet bs = Reflection.as(o, BitSet.class);
            if (bs != null)
            {
                m_array.set(i, new Offline(m_heap, bs));
            }
        }
    }
}
