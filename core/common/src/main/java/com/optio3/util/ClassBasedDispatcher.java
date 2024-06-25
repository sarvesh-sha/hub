/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.util.function.ConsumerWithException;

/**
 * A class-based dispatch table.
 * <p>
 * Useful for routing processing of requests.
 */
public class ClassBasedDispatcher
{
    private static class Entry<T>
    {
        private final Class<T>                 m_clz;
        private final ConsumerWithException<T> m_callback;
        private final boolean                  m_exactMatch;

        Entry(Class<T> clz,
              boolean exactMatch,
              ConsumerWithException<T> callback)
        {
            m_clz = clz;
            m_callback = callback;
            m_exactMatch = exactMatch;
        }

        void call(Object obj) throws
                              Exception
        {
            T t = m_clz.cast(obj);

            m_callback.accept(t);
        }

        public boolean onlyExactMatch()
        {
            return m_exactMatch;
        }
    }

    private final Map<Class<?>, Entry<?>> m_dispatchTable = Maps.newHashMap();

    /**
     * Add a new handler to the dispatch table.
     *
     * @param clz        Target class
     * @param exactMatch If true, only exact type matches will be dispatched to the handler, otherwise any sub-type would do
     * @param callback   The handler to invoke on a type match
     */
    public <T> void add(Class<T> clz,
                        boolean exactMatch,
                        ConsumerWithException<T> callback)
    {
        Entry<T> en = new Entry<>(clz, exactMatch, callback);

        m_dispatchTable.put(clz, en);
    }

    /**
     * Given the type of the object, it try to find a matching handler and
     *
     * @param obj Target object
     *
     * @return true if a matching handler was found
     *
     * @throws Exception
     */
    public boolean dispatch(Object obj) throws
                                        Exception
    {
        boolean  exactMatch = true;
        Class<?> clz        = obj.getClass();
        while (clz != null)
        {
            Entry<?> en = m_dispatchTable.get(clz);
            if (en != null && (exactMatch || !en.onlyExactMatch()))
            {
                en.call(obj);
                return true;
            }

            clz = clz.getSuperclass();
            exactMatch = false;
        }

        return false;
    }
}
