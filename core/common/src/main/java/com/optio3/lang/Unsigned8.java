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
public class Unsigned8 implements Comparable<Unsigned8>
{
    private final long m_value;

    @JsonCreator
    private Unsigned8(long v)
    {
        m_value = v & 0xFF;
    }

    @JsonValue
    long toJsonValue()
    {
        return m_value;
    }

    public static Unsigned8 box(long v)
    {
        return new Unsigned8(v);
    }

    public byte unbox()
    {
        return (byte) m_value;
    }

    public static byte unboxOrDefault(Unsigned8 v,
                                      byte defaultValue)
    {
        return v != null ? v.unbox() : defaultValue;
    }

    public int unboxUnsigned()
    {
        return (int) m_value & 0xFF;
    }

    public static int unboxUnsignedOrDefault(Unsigned8 v,
                                             int defaultValue)
    {
        return v != null ? v.unboxUnsigned() : defaultValue;
    }

    public static Integer unboxOptional(Optional<Unsigned8> opt)
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

        Unsigned8 num2 = Reflection.as(o, Unsigned8.class);
        if (num2 != null)
        {
            return m_value == num2.m_value;
        }

        return false;
    }

    @Override
    public int compareTo(Unsigned8 o)
    {
        return compare(this, o);
    }

    public static int compare(Unsigned8 o1,
                              Unsigned8 o2)
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
