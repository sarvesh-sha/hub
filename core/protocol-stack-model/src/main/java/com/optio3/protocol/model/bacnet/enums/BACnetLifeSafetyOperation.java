/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetLifeSafetyOperation implements TypedBitSet.ValueGetter
{
    // @formatter:off
    none             (0),
    silence          (1),
    silence_audible  (2),
    silence_visual   (3),
    reset            (4),
    reset_alarm      (5),
    reset_fault      (6),
    unsilence        (7),
    unsilence_audible(8),
    unsilence_visual (9);
    // @formatter:on

    private final byte m_encoding;

    BACnetLifeSafetyOperation(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetLifeSafetyOperation parse(byte value)
    {
        for (BACnetLifeSafetyOperation t : values())
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