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
import java.lang.ref.SoftReference;
import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.collection.Memoizer;
import com.optio3.stream.MemoryMappedHeap;

class LookupForAssets
{
    private static class Data
    {
        private final Map<String, Integer> m_fromValueToIndex = Maps.newHashMap();
        private final KeyValuePairArray    m_fromIndexToValue = new KeyValuePairArray();
    }

    private final MemoryMappedHeap m_heap;
    private       long             m_position;
    private       long             m_length;

    private int                 m_count;
    private SoftReference<Data> m_weak;
    private Data                m_strong;

    LookupForAssets(MemoryMappedHeap heap)
    {
        m_heap = heap;
    }

    int put(KeyValuePair pair)
    {
        Data data = ensureDeserialized();

        Integer index = data.m_fromValueToIndex.get(pair.key);
        if (index == null)
        {
            index = data.m_fromIndexToValue.size();
            data.m_fromIndexToValue.add(pair);
            data.m_fromValueToIndex.put(pair.key, index);

            m_position = 0;
            m_length   = 0;

            m_count++;
        }

        return index;
    }

    Integer get(String sysId)
    {
        Data data = ensureDeserialized();

        return data.m_fromValueToIndex.get(sysId);
    }

    KeyValuePair getReverse(int index)
    {
        Data data = ensureDeserialized();

        return data.m_fromIndexToValue.get(index, null);
    }

    int size()
    {
        return m_count;
    }

    //--//

    private Data ensureDeserialized()
    {
        if (m_strong != null)
        {
            return m_strong;
        }

        if (m_weak != null)
        {
            Data strong = m_weak.get();
            if (strong != null)
            {
                return strong;
            }
        }

        Data data = new Data();

        if (m_length != 0)
        {
            Memoizer cache = new Memoizer();

            try
            {
                InputStream     stream    = m_heap.sliceAsInputStream(m_position, m_length);
                DataInputStream dataInput = new DataInputStream(stream);

                int num = dataInput.readInt();

                for (int i = 0; i < num; i++)
                {
                    KeyValuePair pair = KeyValuePair.deserialize(cache, dataInput);
                    pair = cache.intern(pair, KeyValuePair.class);

                    data.m_fromIndexToValue.add(pair);
                    data.m_fromValueToIndex.put(pair.key, i);
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        else
        {
            m_strong = data;
        }

        m_weak = new SoftReference<>(data);
        return data;
    }

    void ensureSerialized() throws
                            IOException
    {
        if (m_strong != null && m_length == 0)
        {
            try (MemoryMappedHeap.SkippableOutputStream stream = m_heap.allocateAsOutputStream())
            {
                DataOutputStream dataOutput = new DataOutputStream(stream);

                int num = m_strong.m_fromIndexToValue.size();
                dataOutput.writeInt(num);

                for (int i = 0; i < num; i++)
                {
                    KeyValuePair pair = m_strong.m_fromIndexToValue.get(i, null);

                    pair.serialize(dataOutput);
                }

                m_position = stream.absolutePosition();
                m_length   = (int) stream.position();

                m_weak   = new SoftReference<>(m_strong);
                m_strong = null;
            }
        }
    }
}
