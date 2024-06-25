/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnParticulateFilterRegenerationState implements TypedBitSet.ValueGetter
{
    // @formatter:off
    NotActive         (0),
    Active            (1),
    RegenerationNeeded(2),
    NotAvailable      (3);
    // @formatter:on

    private final byte m_encoding;

    PgnParticulateFilterRegenerationState(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnParticulateFilterRegenerationState parse(byte value)
    {
        for (PgnParticulateFilterRegenerationState t : values())
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
