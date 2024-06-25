/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnTemperatureRange implements TypedBitSet.ValueGetter
{
    // @formatter:off
    HighMostSevere (0),
    HighLeastSevere(1),
    InRange        (2),
    LowLeastSevere (3),
    LowMostSevere  (4),
    Reserved5      (5),
    Error          (6),
    NotAvailable   (7);
    // @formatter:on

    private final byte m_encoding;

    PgnTemperatureRange(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnTemperatureRange parse(byte value)
    {
        for (PgnTemperatureRange t : values())
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
