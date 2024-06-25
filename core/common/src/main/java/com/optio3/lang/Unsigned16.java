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

@SwaggerTypeReplacement(targetElement = Integer.class)
public class Unsigned16 implements Comparable<Unsigned16>
{
    private final long m_value;

    @JsonCreator
    private Unsigned16(long v)
    {
        m_value = v & 0xFFFF;
    }

    @JsonValue
    long toJsonValue()
    {
        return m_value;
    }

    public static Unsigned16 box(long v)
    {
        return new Unsigned16(v);
    }

    public short unbox()
    {
        return (short) m_value;
    }

    public static short unboxOrDefault(Unsigned16 v,
                                       short defaultValue)
    {
        return v != null ? v.unbox() : defaultValue;
    }

    public int unboxUnsigned()
    {
        return (int) m_value & 0xFFFF;
    }

    public static int unboxUnsignedOrDefault(Unsigned16 v,
                                             int defaultValue)
    {
        return v != null ? v.unboxUnsigned() : defaultValue;
    }

    public static Integer unboxOptional(Optional<Unsigned16> opt)
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

        Unsigned16 num2 = Reflection.as(o, Unsigned16.class);
        if (num2 != null)
        {
            return m_value == num2.m_value;
        }

        return false;
    }

    @Override
    public int compareTo(Unsigned16 o)
    {
        return compare(this, o);
    }

    public static int compare(Unsigned16 o1,
                              Unsigned16 o2)
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
