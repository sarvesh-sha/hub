/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnCruiseControlMode implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Off                (0),
    Hold               (1),
    Accelerate         (2),
    Decelerate         (3),
    Resume             (4),
    Set                (5),
    AcceleratorOverride(6),
    NotAvailable       (7);
    // @formatter:on

    private final byte m_encoding;

    PgnCruiseControlMode(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnCruiseControlMode parse(byte value)
    {
        for (PgnCruiseControlMode t : values())
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
