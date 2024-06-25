/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnStarterConsent implements TypedBitSet.ValueGetter
{
    // @formatter:off
    NoConsent                          (0),
    ConsentToOperatorRequestedStartOnly(1),
    ConsentToAutomaticStartOnly        (2),
    ConsentToBoth                      (3),
    Reserved4                          (4),
    Reserved5                          (5),
    Reserved6                          (6),
    NotAvailable                       (7);
    // @formatter:on

    private final byte m_encoding;

    PgnStarterConsent(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnStarterConsent parse(byte value)
    {
        for (PgnStarterConsent t : values())
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
