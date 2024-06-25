/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.collection;

import java.time.ZonedDateTime;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;

/**
 * A helper class to reuse memory when dealing with large amounts of strings, dates, other immutable objects.
 */
public class Memoizer
{
    private final ConcurrentMap<String, String>               m_strings = Maps.newConcurrentMap();
    private final ConcurrentMap<ZonedDateTime, ZonedDateTime> m_dates   = Maps.newConcurrentMap();
    private final ConcurrentMap<Object, Object>               m_other   = Maps.newConcurrentMap();

    public String intern(String val)
    {
        if (val == null)
        {
            return null;
        }

        String internedValue = m_strings.get(val);
        if (internedValue != null)
        {
            return internedValue;
        }

        String oldVal = m_strings.putIfAbsent(val, val);
        return oldVal != null ? oldVal : val;
    }

    public ZonedDateTime intern(ZonedDateTime val)
    {
        if (val == null)
        {
            return null;
        }

        ZonedDateTime internedValue = m_dates.get(val);
        if (internedValue != null)
        {
            return internedValue;
        }

        ZonedDateTime oldVal = m_dates.putIfAbsent(val, val);
        return oldVal != null ? oldVal : val;
    }

    public <T> T intern(T val,
                        Class<T> clz)
    {
        if (val == null)
        {
            return null;
        }

        Object internedValue = m_other.get(val);
        if (internedValue != null)
        {
            return clz.cast(internedValue);
        }

        Object oldVal = m_other.putIfAbsent(val, val);
        return clz.cast(oldVal != null ? oldVal : val);
    }
}
