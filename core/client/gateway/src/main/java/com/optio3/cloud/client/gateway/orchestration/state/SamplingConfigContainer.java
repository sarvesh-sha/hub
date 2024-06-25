/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.orchestration.state;

import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.serialization.Reflection;

/**
 * This container switches between a Map and an Array, depending on the access pattern.
 * All to reduce steady memory consumption.
 *
 * @param <TKey>
 * @param <TValue>
 */
class SamplingConfigContainer<TKey, TValue extends ISamplingConfig<TKey, TValue>>
{
    private static final ISamplingConfig<?, ?>[] s_empty = new ISamplingConfig[0];

    private Object m_state;

    //--//

    public void clear()
    {
        m_state = null;
    }

    public TValue find(TKey key)
    {
        final Map<TKey, TValue> map = asMap();

        return map.get(key);
    }

    public void add(TValue value)
    {
        final Map<TKey, TValue> map = asMap();

        map.put(value.getKey(), value);
    }

    public void remove(TKey key)
    {
        final Map<TKey, TValue> map = asMap();

        map.remove(key);
    }

    public boolean isEmpty()
    {
        if (m_state == null)
        {
            return true;
        }

        final Map<TKey, TValue> map = asMapNoConvert();
        if (map != null && map.isEmpty())
        {
            return true;
        }

        final ISamplingConfig<TKey, TValue>[] array = asArrayNoConvert();
        if (array != null && array.length == 0)
        {
            return true;
        }

        return false;
    }

    //--//

    Map<TKey, TValue> asMap()
    {
        Map<TKey, TValue> map = asMapNoConvert();
        if (map == null)
        {
            map = Maps.newHashMap();

            final ISamplingConfig<TKey, TValue>[] array = asArrayNoConvert();
            if (array != null)
            {
                for (ISamplingConfig<TKey, TValue> v : array)
                {
                    @SuppressWarnings("unchecked") TValue v2 = (TValue) v;
                    map.put(v.getKey(), v2);
                }
            }

            m_state = map;
        }

        return map;
    }

    ISamplingConfig<TKey, TValue>[] asArray()
    {
        final ISamplingConfig<TKey, TValue>[] array = asArrayNoConvert();
        if (array != null)
        {
            return array;
        }

        final Map<TKey, TValue> map = asMapNoConvert();
        if (map != null)
        {
            @SuppressWarnings("unchecked") ISamplingConfig<TKey, TValue>[] arrayNew = new ISamplingConfig[map.size()];
            int                                                            pos      = 0;

            for (TValue v : map.values())
            {
                arrayNew[pos++] = v;
            }

            m_state = arrayNew;
            return arrayNew;
        }

        @SuppressWarnings("unchecked") ISamplingConfig<TKey, TValue>[] emptyArray = (ISamplingConfig<TKey, TValue>[]) s_empty;
        return emptyArray;
    }

    //--//

    @SuppressWarnings("unchecked")
    private Map<TKey, TValue> asMapNoConvert()
    {
        return Reflection.as(m_state, Map.class);
    }

    @SuppressWarnings("unchecked")
    private ISamplingConfig<TKey, TValue>[] asArrayNoConvert()
    {
        return Reflection.as(m_state, ISamplingConfig[].class);
    }
}
