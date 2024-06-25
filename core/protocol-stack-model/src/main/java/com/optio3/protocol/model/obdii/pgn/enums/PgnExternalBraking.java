/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnExternalBraking implements TypedBitSet.ValueGetter
{
    // @formatter:off
    NoBrakeDemand    (0),
    DriverBrakeDemand(1),
    AdditionMode     (2),
    MaximumMode      (3),
    Reserved4        (4),
    Reserved5        (5),
    Reserved6        (6),
    Reserved7        (7),
    Reserved8        (8),
    Reserved9        (9),
    Reserved10       (10),
    Reserved11       (11),
    Reserved12       (12),
    Reserved13       (13),
    Reserved14       (14),
    NotAvailable     (15);
    // @formatter:on

    private final byte m_encoding;

    PgnExternalBraking(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnExternalBraking parse(byte value)
    {
        for (PgnExternalBraking t : values())
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
