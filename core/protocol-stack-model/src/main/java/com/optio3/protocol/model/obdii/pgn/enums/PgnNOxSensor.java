/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnNOxSensor implements TypedBitSet.ValueGetter
{
    // @formatter:off
    DiagnosisNotActive         (0),
    SelfDiagnosisActiveFlag    (1),
    SelfDiagnosisResultComplete(2),
    SelfDiagnosisAborted       (3),
    SelfDiagnosisNotPossible   (4),
    Reserved5                  (5),
    Reserved6                  (6),
    NotAvailable               (7);
    // @formatter:on

    private final byte m_encoding;

    PgnNOxSensor(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnNOxSensor parse(byte value)
    {
        for (PgnNOxSensor t : values())
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
