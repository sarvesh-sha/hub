/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.morningstar;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum TriStarLed implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Start(0),
    Start2(1),
    Branch(2),
    GreenBlinkFast(3),
    GreenBlinkSlow(4),
    GreenBlink(5),
    Green(6),
    Yellow(8),
    RedBlink(10),
    Red(11),
    Error_RYG(12),
    Error_R_YG(13),
    Error_R_GY(14),
    Error_RY_HTD(15),
    Error_RG_HVD(16),
    Error_R_YG_Y(17),
    Error_G_Y_R(18),
    Error_G_Y_R_2x(19);
    // @formatter:on

    private final byte m_encoding;

    TriStarLed(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static TriStarLed parse(byte value)
    {
        for (TriStarLed t : values())
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
