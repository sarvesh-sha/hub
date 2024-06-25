/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.morningstar;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum TriStarCharger implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Start(0),
    NightCheck(1),
    Disconnect(2),
    Night(3),
    Fault(4),
    MPPT(5),
    Absorption(6),
    Float(7),
    Equalize(8),
    Slave(9);
    // @formatter:on

    private final byte m_encoding;

    TriStarCharger(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static TriStarCharger parse(byte value)
    {
        for (TriStarCharger t : values())
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
