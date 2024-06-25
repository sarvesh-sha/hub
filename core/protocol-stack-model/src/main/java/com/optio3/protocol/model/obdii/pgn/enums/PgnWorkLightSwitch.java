/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnWorkLightSwitch implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Off         (0),
    Combination1(1),
    Combination2(2),
    Combination3(3),
    Combination4(4),
    Reserved5   (5),
    Reserved6   (6),
    Reserved7   (7),
    Reserved8   (8),
    Reserved9   (9),
    Reserved10  (10),
    Reserved11  (11),
    Reserved12  (12),
    Reserved13  (13),
    Error       (14),
    NotAvailable(15);
    // @formatter:on

    private final byte m_encoding;

    PgnWorkLightSwitch(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnWorkLightSwitch parse(byte value)
    {
        for (PgnWorkLightSwitch t : values())
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
