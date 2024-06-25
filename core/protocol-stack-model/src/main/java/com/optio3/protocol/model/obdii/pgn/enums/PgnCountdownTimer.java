/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnCountdownTimer implements TypedBitSet.ValueGetter
{
    // @formatter:off
    LessThanOneMinute(0),
    OneMinute        (1),
    TwoMinutes       (2),
    ThreeMinutes     (3),
    FourMinutes      (4),
    FiveMinutes      (5),
    SixMinutes       (6),
    SevenMinutes     (7),
    EightMinutes     (8),
    NineMinutes      (9),
    TenMinutes       (10),
    ElevenMinutes    (11),
    TwelveMinutes    (12),
    ThirteenMinutes  (13),
    Error            (14),
    NotAvailable     (15);
    // @formatter:on

    private final byte m_encoding;

    PgnCountdownTimer(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnCountdownTimer parse(byte value)
    {
        for (PgnCountdownTimer t : values())
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
