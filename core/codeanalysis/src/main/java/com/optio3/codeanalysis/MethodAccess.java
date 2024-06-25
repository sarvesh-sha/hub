/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import java.util.EnumSet;
import java.util.Set;

import org.objectweb.asm.Opcodes;

public enum MethodAccess
{
    // @formatter:off
    Public      (Opcodes.ACC_PUBLIC      ),
    Private     (Opcodes.ACC_PRIVATE     ),
    Protected   (Opcodes.ACC_PROTECTED   ),
    Static      (Opcodes.ACC_STATIC      ),
    Final       (Opcodes.ACC_FINAL       ),
    Synchronized(Opcodes.ACC_SYNCHRONIZED),
    Bridge      (Opcodes.ACC_BRIDGE      ),
    Varargs     (Opcodes.ACC_VARARGS     ),
    Native      (Opcodes.ACC_NATIVE      ),
    Abstract    (Opcodes.ACC_ABSTRACT    ),
    Strict      (Opcodes.ACC_STRICT      ),
    Synthetic   (Opcodes.ACC_SYNTHETIC   ),
    Deprecated  (Opcodes.ACC_DEPRECATED  );
    // @formatter:on

    private final int m_mask;

    MethodAccess(int mask)
    {
        m_mask = mask;
    }

    public static MethodAccess parse(int value)
    {
        for (MethodAccess e : values())
        {
            if (e.m_mask == value)
            {
                return e;
            }
        }

        return null;
    }

    public static Set<MethodAccess> fromValue(int access)
    {
        EnumSet<MethodAccess> res = EnumSet.noneOf(MethodAccess.class);

        for (MethodAccess e : values())
        {
            if ((e.m_mask & access) != 0)
            {
                res.add(e);
            }
        }

        return res;
    }

    public static int toValue(Set<MethodAccess> set)
    {
        int value = 0;

        for (MethodAccess e : set)
            value |= e.m_mask;

        return value;
    }

    public int encoding()
    {
        return m_mask;
    }
}
