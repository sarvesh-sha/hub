/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.util.EnumSet;
import java.util.Set;

import org.objectweb.asm.Opcodes;

public enum ClassAccess
{
    // @formatter:off
    Public    (Opcodes.ACC_PUBLIC    ),
    Private   (Opcodes.ACC_PRIVATE   ),
    Protected (Opcodes.ACC_PROTECTED ),
    Static    (Opcodes.ACC_STATIC    ),
    Final     (Opcodes.ACC_FINAL     ),
    Super     (Opcodes.ACC_SUPER     ),
    Interface (Opcodes.ACC_INTERFACE ),
    Abstract  (Opcodes.ACC_ABSTRACT  ),
    Synthetic (Opcodes.ACC_SYNTHETIC ),
    Annotation(Opcodes.ACC_ANNOTATION),
    Enum      (Opcodes.ACC_ENUM      ),
    Deprecated(Opcodes.ACC_DEPRECATED);
    // @formatter:on

    private final int m_mask;

    ClassAccess(int mask)
    {
        m_mask = mask;
    }

    public static ClassAccess parse(int value)
    {
        for (ClassAccess e : values())
        {
            if (e.m_mask == value)
            {
                return e;
            }
        }

        return null;
    }

    public static Set<ClassAccess> fromValue(int access)
    {
        EnumSet<ClassAccess> res = EnumSet.noneOf(ClassAccess.class);

        for (ClassAccess e : values())
        {
            if ((e.m_mask & access) != 0)
            {
                res.add(e);
            }
        }

        return res;
    }

    public static int toValue(Set<ClassAccess> set)
    {
        int value = 0;

        for (ClassAccess e : set)
            value |= e.m_mask;

        return value;
    }

    public int encoding()
    {
        return m_mask;
    }
}
