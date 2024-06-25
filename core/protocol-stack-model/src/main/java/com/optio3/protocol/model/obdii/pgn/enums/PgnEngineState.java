/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnEngineState implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Stopped   (0),
    PreStart  (1),
    Starting  (2),
    WarmUp    (3),
    Running   (4),
    CoolDown  (5),
    Stopping  (6),
    PostRun   (7),
    Reserved8 (8),
    Reserved9 (9),
    Reserved10(10),
    Reserved11(11),
    Reserved12(12),
    Reserved13(13),
    Reserved14(14),
    DontCare  (15);
    // @formatter:on

    private final byte m_encoding;

    PgnEngineState(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnEngineState parse(byte value)
    {
        for (PgnEngineState t : values())
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
