/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnHillHolder implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Inactive                              (0),
    Active                                (1),
    Active_WillChangeToInactiveInShortTime(2),
    Active_MayActivateParkingBrake        (3),
    Reserved4                             (4),
    Reserved5                             (5),
    NotFunctional                         (6),
    NotAvailable                          (7);
    // @formatter:on

    private final byte m_encoding;

    PgnHillHolder(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnHillHolder parse(byte value)
    {
        for (PgnHillHolder t : values())
        {
            if (t.m_encoding == value)
            {
                return t;
            }
        }

        return null;
    }

    @HandlerForEncoding
    public byte encoding()
    {
        return m_encoding;
    }

    @Override
    public int getEncodingValue()
    {
        return m_encoding;
    }
}
