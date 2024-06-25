/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnStarterRequest implements TypedBitSet.ValueGetter
{
    // @formatter:off
    NotRequested       (0),
    Requested_Operator (1),
    Requested_Automatic(2),
    NotAvailable       (3);
    // @formatter:on

    private final byte m_encoding;

    PgnStarterRequest(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnStarterRequest parse(byte value)
    {
        for (PgnStarterRequest t : values())
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
