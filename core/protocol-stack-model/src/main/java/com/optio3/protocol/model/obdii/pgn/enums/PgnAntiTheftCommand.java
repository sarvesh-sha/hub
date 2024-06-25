/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnAntiTheftCommand implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Add_Password   (0),
    Delete_Password(1),
    Change_Password(2),
    Lock_or_Unlock (3),
    Check_Status   (4),
    Login          (5),
    Reserved6      (6),
    Reserved7      (7);
    // @formatter:on

    private final byte m_encoding;

    PgnAntiTheftCommand(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnAntiTheftCommand parse(byte value)
    {
        for (PgnAntiTheftCommand t : values())
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
