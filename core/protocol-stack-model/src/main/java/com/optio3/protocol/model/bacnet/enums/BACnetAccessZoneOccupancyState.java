/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetAccessZoneOccupancyState implements TypedBitSet.ValueGetter
{
    // @formatter:off
    normal           (0),
    below_lower_limit(1),
    at_lower_limit   (2),
    at_upper_limit   (3),
    above_upper_limit(4),
    disabled         (5),
    not_supported    (6);
    // @formatter:on

    private final byte m_encoding;

    BACnetAccessZoneOccupancyState(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetAccessZoneOccupancyState parse(byte value)
    {
        for (BACnetAccessZoneOccupancyState t : values())
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
