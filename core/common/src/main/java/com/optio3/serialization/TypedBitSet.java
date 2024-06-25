/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.serialization;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.Lists;
import com.optio3.util.BitSets;

public abstract class TypedBitSet<T extends Enum<T> & TypedBitSet.ValueGetter>
{
    public interface ValueGetter
    {
        int getEncodingValue();
    }

    private final BitSet   m_data;
    private final Class<T> m_sourceEnum;
    private final T[]      m_values;

    protected TypedBitSet(Class<T> sourceEnum,
                          T[] values)
    {
        m_data = new BitSet();

        m_sourceEnum = sourceEnum;
        m_values = values;
    }

    protected TypedBitSet(Class<T> sourceEnum,
                          T[] values,
                          BitSet bs)
    {
        this(sourceEnum, values);

        m_data.or(bs);
    }

    protected TypedBitSet(Class<T> sourceEnum,
                          T[] values,
                          long val)
    {
        this(sourceEnum, values);

        for (T value : values)
        {
            final int offset = value.getEncodingValue();
            if ((val & (1L << offset)) != 0)
            {
                m_data.set(offset);
            }
        }
    }

    protected TypedBitSet(Class<T> sourceEnum,
                          T[] values,
                          List<String> inputs)
    {
        this(sourceEnum, values);

        for (T value : values)
        {
            if (inputs.contains(value.name()))
            {
                set(value);
            }
        }
    }

    @Override
    public boolean equals(Object o)
    {
        TypedBitSet<?> that = Reflection.as(o, TypedBitSet.class);
        if (that == null)
        {
            return false;
        }

        return m_data.equals(that.m_data);
    }

    @Override
    public int hashCode()
    {
        return m_data.hashCode();
    }

    //--//

    public boolean isSet(T val)
    {
        return m_data.get(val.getEncodingValue());
    }

    public void set(T val)
    {
        m_data.set(val.getEncodingValue());
    }

    public void clear(T val)
    {
        m_data.clear(val.getEncodingValue());
    }

    public EnumSet<T> values()
    {
        EnumSet<T> res = EnumSet.noneOf(m_sourceEnum);

        for (T e : m_values)
        {
            if (isSet(e))
            {
                res.add(e);
            }
        }

        return res;
    }

    public int length()
    {
        int max = -1;

        for (T value : m_values)
            max = Math.max(max, value.getEncodingValue() + 1);

        return max;
    }

    public BitSet toBitSet()
    {
        return BitSets.copy(m_data);
    }

    @JsonValue
    public List<String> toJsonValue()
    {
        List<String> res = Lists.newArrayList();

        for (T v : values())
        {
            res.add(v.name());
        }

        return res;
    }

    public static BitSet extract(Object value)
    {
        TypedBitSet<?> tbs = Reflection.as(value, TypedBitSet.class);
        if (tbs != null)
        {
            return tbs.toBitSet();
        }

        return (BitSet) value;
    }

    //--//

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (T v : values())
        {
            if (sb.length() == 0)
            {
                sb.append("[");
            }
            else
            {
                sb.append(", ");
            }
            sb.append(v.name());
        }

        if (sb.length() == 0)
        {
            sb.append("[");
        }

        sb.append("]");
        return sb.toString();
    }
}
