/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnInducementAnomaly implements TypedBitSet.ValueGetter
{
    // @formatter:off
    NotActive        (0),
    InducementLevel1 (1),
    InducementLevel2 (2),
    InducementLevel3 (3),
    InducementLevel4 (4),
    InducementLevel5 (5),
    TemporaryOverride(6),
    NotAvailable     (7);
    // @formatter:on

    private final byte m_encoding;

    PgnInducementAnomaly(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnInducementAnomaly parse(byte value)
    {
        for (PgnInducementAnomaly t : values())
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
