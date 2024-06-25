/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetEventState implements TypedBitSet.ValueGetter
{
    // @formatter:off
    normal           (0),
    fault            (1),
    offnormal        (2),
    high_limit       (3),
    low_limit        (4),
    life_safety_alarm(5);
    // @formatter:on

    private final byte m_encoding;

    BACnetEventState(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetEventState parse(byte value)
    {
        for (BACnetEventState t : values())
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
