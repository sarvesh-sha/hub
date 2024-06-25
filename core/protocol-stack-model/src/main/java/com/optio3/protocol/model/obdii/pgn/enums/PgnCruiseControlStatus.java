/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnCruiseControlStatus implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Off                     (0),
    DisabledByDisableCommand(1),
    DisabledByPauseCommand  (2),
    ActivatedByResumeCommand(3),
    ResumeCommand           (4),
    SetCommand              (5),
    Reserved6               (6),
    NotAvailable            (7);
    // @formatter:on

    private final byte m_encoding;

    PgnCruiseControlStatus(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnCruiseControlStatus parse(byte value)
    {
        for (PgnCruiseControlStatus t : values())
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
