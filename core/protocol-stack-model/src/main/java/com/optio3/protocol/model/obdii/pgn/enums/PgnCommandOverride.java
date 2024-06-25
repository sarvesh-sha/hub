/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnCommandOverride implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Allow       (0),
    Disable     (1),
    Request     (2),
    TakeNoAction(3);
    // @formatter:on

    private final byte m_encoding;

    PgnCommandOverride(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnCommandOverride parse(byte value)
    {
        for (PgnCommandOverride t : values())
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
