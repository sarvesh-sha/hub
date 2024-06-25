/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.collection;

import static java.util.Objects.requireNonNull;

import java.lang.ref.SoftReference;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Maps;
import com.optio3.serialization.Reflection;

public class MapWithSoftValues<K, V> implements Map<K, V>
{
    private final Map<K, SoftReference<V>> m_map;

    public MapWithSoftValues()
    {
        m_map = Maps.newHashMap();
    }

    @Override
    public int size()
    {
        return m_map.size();
    }

    @Override
    public boolean isEmpty()
    {
        return m_map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value)
    {
        if (value != null)
        {
            for (V val : values())
            {
                if (Objects.equals(val, value))
                {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public V get(Object key)
    {
        return unwrap(m_map.get(key));
    }

    @Override
    public V put(K key,
                 V value)
    {
        requireNonNull(value);

        return unwrap(m_map.put(key, new SoftReference<>(value)));
    }

    @Override
    public V remove(Object key)
    {
        return unwrap(m_map.remove(key));
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m)
    {
        for (K key : m.keySet())
        {
            put(key, m.get(key));
        }
    }

    @Override
    public void clear()
    {
        m_map.clear();
    }

    @Override
    public Set<K> keySet()
    {
        return new Keys();
    }

    @Override
    public Collection<V> values()
    {
        return new Values();
    }

    @Override
    public Set<Entry<K, V>> entrySet()
    {
        return new EntrySet();
    }

    //--//

    private V unwrap(SoftReference<V> v)
    {
        return v != null ? v.get() : null;
    }

    final class Keys extends AbstractSet<K>
    {
        public final int size()
        {
            return MapWithSoftValues.this.size();
        }

        public final void clear()
        {
            MapWithSoftValues.this.clear();
        }

        public final Iterator<K> iterator()
        {
            return new KeyIterator();
        }

        public final boolean contains(Object o)
        {
            return containsKey(o);
        }
    }

    final class Values extends AbstractCollection<V>
    {
        public final int size()
        {
            return MapWithSoftValues.this.size();
        }

        public final void clear()
        {
            MapWithSoftValues.this.clear();
        }

        public final Iterator<V> iterator()
        {
            return new ValueIterator();
        }

        public final boolean contains(Object o)
        {
            return containsValue(o);
        }
    }

    abstract class BaseIterator<E> implements Iterator<E>
    {
        private final Iterator<Entry<K, SoftReference<V>>> m_it;

        Entry<K, SoftReference<V>> m_next;
        V                          m_nextValue;

        BaseIterator()
        {
            m_it = m_map.entrySet()
                        .iterator();
        }

        @Override
        public boolean hasNext()
        {
            while (m_it.hasNext())
            {
                m_next = m_it.next();

                m_nextValue = m_next.getValue()
                                    .get();

                if (m_nextValue != null)
                {
                    return true;
                }

                // The entry got garbage collected, remove it.
                m_it.remove();
            }

            return false;
        }
    }

    final class KeyIterator extends BaseIterator<K>
    {
        public final K next()
        {
            return m_next.getKey();
        }
    }

    final class ValueIterator extends BaseIterator<V>
    {
        public final V next()
        {
            return m_nextValue;
        }
    }

    final class EntryIterator extends BaseIterator<Entry<K, V>>
    {
        public final Entry<K, V> next()
        {
            return new Entry<K, V>()
            {
                @Override
                public K getKey()
                {
                    return m_next.getKey();
                }

                @Override
                public V getValue()
                {
                    return m_nextValue;
                }

                @Override
                public V setValue(V value)
                {
                    V oldValue = m_nextValue;

                    m_nextValue = value;
                    m_next.setValue(new SoftReference<>(value));

                    return oldValue;
                }
            };
        }
    }

    final class EntrySet extends AbstractSet<Entry<K, V>>
    {
        public final int size()
        {
            return MapWithSoftValues.this.size();
        }

        public final void clear()
        {
            MapWithSoftValues.this.clear();
        }

        public final Iterator<Entry<K, V>> iterator()
        {
            return new EntryIterator();
        }

        public final boolean contains(Object o)
        {
            Map.Entry<?, ?> e = Reflection.as(o, Map.Entry.class);
            if (e != null)
            {
                Object key      = e.getKey();
                Object valueExp = e.getValue();
                Object value    = MapWithSoftValues.this.get(key);

                return Objects.equals(valueExp, value);
            }

            return false;
        }

        public final boolean remove(Object o)
        {
            Map.Entry<?, ?> e = Reflection.as(o, Map.Entry.class);
            if (e != null)
            {
                Object key      = e.getKey();
                Object valueExp = e.getValue();
                Object value    = MapWithSoftValues.this.get(key);

                if (Objects.equals(valueExp, value))
                {
                    m_map.remove(key);
                    return true;
                }
            }

            return false;
        }
    }
}
