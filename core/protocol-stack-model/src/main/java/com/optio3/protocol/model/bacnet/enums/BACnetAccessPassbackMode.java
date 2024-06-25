/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetAccessPassbackMode implements TypedBitSet.ValueGetter
{
    // @formatter:off
    passback_off (0),
    hard_passback(1),
    soft_passback(2);
    // @formatter:on

    private final byte m_encoding;

    BACnetAccessPassbackMode(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetAccessPassbackMode parse(byte value)
    {
        for (BACnetAccessPassbackMode t : values())
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
