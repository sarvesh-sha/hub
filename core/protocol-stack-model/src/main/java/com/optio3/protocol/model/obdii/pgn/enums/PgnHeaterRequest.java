/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnHeaterRequest implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Off             (0),
    OffDueToADR     (1),
    EconomyMode     (2),
    NormalMode      (3),
    HeaterPumpUpKeep(4),
    Reserved5       (5),
    Reserved6       (6),
    Reserved7       (7),
    Reserved8       (8),
    Reserved9       (9),
    Reserved10      (10),
    Reserved11      (11),
    Reserved12      (12),
    Reserved13      (13),
    Reserved14      (14),
    DontCare        (15);
    // @formatter:on

    private final byte m_encoding;

    PgnHeaterRequest(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnHeaterRequest parse(byte value)
    {
        for (PgnHeaterRequest t : values())
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
