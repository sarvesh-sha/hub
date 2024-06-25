/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnCruiseControlEnable implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Allowed   (0),
    NotAllowed(1),
    Reserved  (2),
    DontCare  (3);
    // @formatter:on

    private final byte m_encoding;

    PgnCruiseControlEnable(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnCruiseControlEnable parse(byte value)
    {
        for (PgnCruiseControlEnable t : values())
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
