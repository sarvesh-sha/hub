/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetLiftGroupMode implements TypedBitSet.ValueGetter
{
    // @formatter:off
    unknown        (0),
    normal         (1),
    down_peak      (2),
    two_way        (3),
    four_way       (4),
    emergency_power(5),
    up_peak        (6);
    // @formatter:on

    private final byte m_encoding;

    BACnetLiftGroupMode(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetLiftGroupMode parse(byte value)
    {
        for (BACnetLiftGroupMode t : values())
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
