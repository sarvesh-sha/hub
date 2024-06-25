/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.util.EnumSet;

import org.objectweb.asm.Opcodes;

public enum ParameterAccess
{
    // @formatter:off
    Final    (Opcodes.ACC_FINAL    ),
    Synthetic(Opcodes.ACC_SYNTHETIC),
    Mandated (Opcodes.ACC_MANDATED );
    // @formatter:on

    private final int m_mask;

    ParameterAccess(int mask)
    {
        m_mask = mask;
    }

    public static ParameterAccess parse(int value)
    {
        for (ParameterAccess e : values())
        {
            if (e.m_mask == value)
            {
                return e;
            }
        }

        return null;
    }

    public static EnumSet<ParameterAccess> fromValue(int access)
    {
        EnumSet<ParameterAccess> res = EnumSet.noneOf(ParameterAccess.class);

        for (ParameterAccess e : values())
        {
            if ((e.m_mask & access) != 0)
            {
                res.add(e);
            }
        }

        return res;
    }

    public static int toValue(EnumSet<ParameterAccess> set)
    {
        int value = 0;

        for (ParameterAccess e : set)
            value |= e.m_mask;

        return value;
    }

    public int encoding()
    {
        return m_mask;
    }
}
