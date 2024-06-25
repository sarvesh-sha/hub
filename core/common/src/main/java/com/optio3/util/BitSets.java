/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.util.BitSet;

public class BitSets
{
    public static BitSet copy(BitSet bs)
    {
        return BitSet.valueOf(bs.toLongArray());
    }

    public static BitSet and(BitSet a,
                             BitSet b)
    {
        BitSet res = copy(a);
        res.and(b);
        return res;
    }

    public static BitSet or(BitSet a,
                            BitSet b)
    {
        BitSet res = copy(a);
        res.or(b);
        return res;
    }

    public static BitSet xor(BitSet a,
                             BitSet b)
    {
        BitSet res = copy(a);
        res.xor(b);
        return res;
    }

    public static BitSet not(BitSet a,
                             int length)
    {
        BitSet res = copy(a);
        res.flip(0, length - 1);
        return res;
    }
}
