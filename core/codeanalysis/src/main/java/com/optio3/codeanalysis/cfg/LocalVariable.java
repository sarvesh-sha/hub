/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg;

import java.util.Objects;

import com.optio3.codeanalysis.GenericType;
import com.optio3.serialization.Reflection;
import org.objectweb.asm.Type;

public final class LocalVariable
{
    private Integer m_index;

    public final Type type;

    LocalVariable(Integer index,
                  Type type)
    {
        m_index = index;

        this.type = type;
    }

    public SourceCodeVariable createSourceCodeVariable(String name,
                                                       GenericType signature)
    {
        return new SourceCodeVariable(this, name, signature);
    }

    public Integer getIndex()
    {
        return m_index;
    }

    void setIndex(int index)
    {
        m_index = index;
    }

    //--//

    @Override
    public int hashCode()
    {
        return type.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        LocalVariable that = Reflection.as(o, LocalVariable.class);
        if (that == null)
        {
            return false;
        }

        return matches(that);
    }

    public boolean matches(LocalVariable obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!Objects.equals(m_index, obj.m_index))
        {
            return false;
        }

        if (!type.equals(obj.type))
        {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return String.format("%d (%s)", m_index, type);
    }
}
