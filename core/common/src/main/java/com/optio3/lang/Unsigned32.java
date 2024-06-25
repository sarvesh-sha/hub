/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.lang;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.optio3.cloud.client.SwaggerTypeReplacement;
import com.optio3.serialization.Reflection;

@SwaggerTypeReplacement(targetElement = Long.class)
public class Unsigned32 implements Comparable<Unsigned32>
{
    private final long m_value;

    @JsonCreator
    private Unsigned32(long v)
    {
        m_value = v & 0xFFFF_FFFF;
    }

    @JsonValue
    long toJsonValue()
    {
        return m_value;
    }

    public static Unsigned32 box(long v)
    {
        return new Unsigned32(v);
    }

    public int unbox()
    {
        return (int) m_value;
    }

    public static int unboxOrDefault(Unsigned32 v,
                                     int defaultValue)
    {
        return v != null ? v.unbox() : defaultValue;
    }

    public long unboxUnsigned()
    {
        return m_value;
    }

    public static long unboxUnsignedOrDefault(Unsigned32 v,
                                              long defaultValue)
    {
        return v != null ? v.unboxUnsigned() : defaultValue;
    }

    public static Long unboxOptional(Optional<Unsigned32> opt)
    {
        if (opt == null || !opt.isPresent())
        {
            return null;
        }

        return opt.get()
                  .unboxUnsigned();
    }

    //--//

    @Override
    public int hashCode()
    {
        return Long.hashCode(m_value);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        Number num1 = Reflection.as(o, Number.class);
        if (num1 != null)
        {
            return num1.longValue() == m_value;
        }

        Unsigned32 num2 = Reflection.as(o, Unsigned32.class);
        if (num2 != null)
        {
            return m_value == num2.m_value;
        }

        return false;
    }

    @Override
    public int compareTo(Unsigned32 o)
    {
        return compare(this, o);
    }

    public static int compare(Unsigned32 o1,
                              Unsigned32 o2)
    {
        if (o1 == o2)
        {
            return 0;
        }

        if (o1 == null)
        {
            return 1;
        }

        if (o2 == null)
        {
            return -1;
        }

        return Long.compare(o1.m_value, o2.m_value);
    }

    @Override
    public String toString()
    {
        return Long.toString(m_value);
    }
}
