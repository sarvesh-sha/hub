/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.util.EnumSet;
import java.util.Set;

import org.objectweb.asm.Opcodes;

public enum FieldAccess
{
    // @formatter:off
    Public    (Opcodes.ACC_PUBLIC    ),
    Private   (Opcodes.ACC_PRIVATE   ),
    Protected (Opcodes.ACC_PROTECTED ),
    Static    (Opcodes.ACC_STATIC    ),
    Final     (Opcodes.ACC_FINAL     ),
    Volatile  (Opcodes.ACC_VOLATILE  ),
    Transient (Opcodes.ACC_TRANSIENT ),
    Synthetic (Opcodes.ACC_SYNTHETIC ),
    Enum      (Opcodes.ACC_ENUM      ),
    Deprecated(Opcodes.ACC_DEPRECATED);
    // @formatter:on

    private final int m_mask;

    FieldAccess(int mask)
    {
        m_mask = mask;
    }

    public static FieldAccess parse(int value)
    {
        for (FieldAccess e : values())
        {
            if (e.m_mask == value)
            {
                return e;
            }
        }

        return null;
    }

    public static Set<FieldAccess> fromValue(int access)
    {
        EnumSet<FieldAccess> res = EnumSet.noneOf(FieldAccess.class);

        for (FieldAccess e : values())
        {
            if ((e.m_mask & access) != 0)
            {
                res.add(e);
            }
        }

        return res;
    }

    public static int toValue(Set<FieldAccess> set)
    {
        int value = 0;

        for (FieldAccess e : set)
            value |= e.m_mask;

        return value;
    }

    public int encoding()
    {
        return m_mask;
    }
}
