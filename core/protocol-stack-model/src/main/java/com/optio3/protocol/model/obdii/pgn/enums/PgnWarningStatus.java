/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnWarningStatus implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Off         (0),
    OnSolid     (1),
    Reserved2   (2),
    Reserved3   (3),
    OnFastBlink (4),
    Reserved5   (5),
    Reserved6   (6),
    NotAvailable(7);
    // @formatter:on

    private final byte m_encoding;

    PgnWarningStatus(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnWarningStatus parse(byte value)
    {
        for (PgnWarningStatus t : values())
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
