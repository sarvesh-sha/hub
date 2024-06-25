/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.concurrency;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

public class TwoWayConcurrentMap<K, V>
{
    private final ConcurrentMap<K, V> m_forward = Maps.newConcurrentMap();
    private final ConcurrentMap<V, K> m_reverse = Maps.newConcurrentMap();

    public V getForward(K key)
    {
        return m_forward.get(key);
    }

    public K getReverse(V value)
    {
        return m_reverse.get(value);
    }

    public boolean add(K key,
                       V value)
    {
        if (m_forward.putIfAbsent(key, value) == null)
        {
            m_reverse.putIfAbsent(value, key);
            return true;
        }

        return false;
    }

    public boolean remove(K key,
                          V value)
    {
        if (m_forward.containsKey(key) && m_reverse.containsKey(value))
        {
            m_forward.remove(key, value);
            m_reverse.replace(value, key);
            return true;
        }

        return false;
    }

    public boolean containsKey(K key)
    {
        return m_forward.containsKey(key);
    }

    public boolean containsValue(V value)
    {
        return m_reverse.containsKey(value);
    }

    public Set<K> keySet()
    {
        return m_forward.keySet();
    }

    public Set<V> valueSet()
    {
        return m_reverse.keySet();
    }
}
