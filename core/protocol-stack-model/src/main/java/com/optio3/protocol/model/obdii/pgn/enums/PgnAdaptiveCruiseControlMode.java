/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnAdaptiveCruiseControlMode implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Off                  (0),
    SpeedControlActive   (1),
    DistanceControlActive(2),
    Overtake             (3),
    Hold                 (4),
    Finish               (5),
    DisabledOrError      (6),
    NotAvailable         (7);
    // @formatter:on

    private final byte m_encoding;

    PgnAdaptiveCruiseControlMode(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnAdaptiveCruiseControlMode parse(byte value)
    {
        for (PgnAdaptiveCruiseControlMode t : values())
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
