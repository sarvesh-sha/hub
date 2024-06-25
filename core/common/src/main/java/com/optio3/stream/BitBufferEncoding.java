/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.stream;

public class BitBufferEncoding
{
    final static int VariableLength3or5  = 0b0;
    final static int VariableLength7or12 = 0b10;
    final static int VariableLengthN     = 0b11; // plus 4 bits = number of 4bit parts + 1
    final static int VariableLength3     = (VariableLength3or5 << 1) | 0b0;
    final static int VariableLength5     = (VariableLength3or5 << 1) | 0b1;
    final static int VariableLength7     = (VariableLength7or12 << 1) | 0b0;
    final static int VariableLength12    = (VariableLength7or12 << 1) | 0b1;

    public static long extract(long val,
                               int offset,
                               int len)
    {
        long mask = mask(len);

        return ((val >> offset) & mask);
    }

    public static long insert(long target,
                              long value,
                              int offset,
                              int len)
    {
        long mask        = mask(len);
        long shiftedMask = mask << offset;

        target &= ~shiftedMask;
        target |= (value << offset) & shiftedMask;

        return target;
    }

    static long mask(int len)
    {
        return 0xFFFF_FFFF_FFFF_FFFFL >>> (64 - len);
    }
}
