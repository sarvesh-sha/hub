/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetLiftCarDirection implements TypedBitSet.ValueGetter
{
    // @formatter:off
    unknown    (0),
    none       (1),
    stopped    (2),
    up         (3),
    down       (4),
    up_and_down(5);
    // @formatter:on

    private final byte m_encoding;

    BACnetLiftCarDirection(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetLiftCarDirection parse(byte value)
    {
        for (BACnetLiftCarDirection t : values())
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
