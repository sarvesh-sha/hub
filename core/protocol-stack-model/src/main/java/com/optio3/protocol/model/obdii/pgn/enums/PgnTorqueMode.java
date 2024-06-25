/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnTorqueMode implements TypedBitSet.ValueGetter
{
    // @formatter:off
    NotRequested(0),
    Torque1     (1),
    Torque2     (2),
    Torque3     (3),
    Torque4     (4),
    Torque5     (5),
    Torque6     (6),
    Torque7     (7),
    Torque8     (8),
    Torque9     (9),
    Torque10    (10),
    Torque11    (11),
    Torque12    (12),
    Torque13    (13),
    Torque14    (14),
    NotAvailable(15);
    // @formatter:on

    private final byte m_encoding;

    PgnTorqueMode(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnTorqueMode parse(byte value)
    {
        for (PgnTorqueMode t : values())
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
