/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnScrState implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Dormant                  (0),
    PreparingDosingReadiness (1),
    NormalDosingOperation    (2),
    SystemErrorPending       (3),
    Purging                  (4),
    ProtectModeAgainstHeat   (5),
    ProtectModeAgainstCold   (6),
    Shutoff                  (7),
    Diagnosis                (8),
    TestMode_DosingAllowed   (9),
    TestMode_DosingNotAllowed(10),
    OkToPowerDown            (11),
    Priming                  (12),
    Reserved13               (13),
    Error14                  (14),
    NotAvailable             (15);
    // @formatter:on

    private final byte m_encoding;

    PgnScrState(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnScrState parse(byte value)
    {
        for (PgnScrState t : values())
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
