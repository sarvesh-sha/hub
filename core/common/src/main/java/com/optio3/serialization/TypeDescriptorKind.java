/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.serialization;

public enum TypeDescriptorKind
{
    // @formatter:off
    integerSigned  (true, true),
    integerUnsigned(true, false),
    floatingPoint  (false, true);
    // @formatter:on

    public final boolean isInteger;
    public final boolean isSigned;

    TypeDescriptorKind(boolean isInteger,
                       boolean isSigned)
    {
        this.isInteger = isInteger;
        this.isSigned = isSigned;
    }

    public long signAwareMasking(long v,
                                 int maskWidth)
    {
        int shift = 64 - maskWidth;

        v <<= shift;

        return signAwareRightShift(v, shift);
    }

    public long signAwareRightShift(long v,
                                    int shift)
    {
        if (isSigned)
        {
            v >>= shift;
        }
        else
        {
            v >>>= shift;
        }

        return v;
    }
}
