/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.serialization;

public abstract class TypeDescriptor
{
    public final Class<?>           clz;
    public final int                size;
    public final TypeDescriptorKind kind;
    private      Class<?>           m_clzBoxed;

    public TypeDescriptor(Class<?> type,
                          int size,
                          TypeDescriptorKind kind)
    {
        this.clz = type;
        this.size = size;
        this.kind = kind;
    }

    public boolean isFloatingType()
    {
        return kind == TypeDescriptorKind.floatingPoint;
    }

    public boolean isPrimitive()
    {
        return clz.isPrimitive();
    }

    public Class<?> getBoxedType()
    {
        return m_clzBoxed;
    }

    void setBoxedType(Class<?> clz)
    {
        m_clzBoxed = clz;
    }

    public abstract Object fromLongValue(long value);

    public abstract long asLongValue(Object value);
}
