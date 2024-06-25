/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetProtocolLevel implements TypedBitSet.ValueGetter
{
    // @formatter:off
    physical              (0),
    protocol              (1),
    bacnet_application    (2),
    non_bacnet_application(3);
    // @formatter:on

    private final byte m_encoding;

    BACnetProtocolLevel(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetProtocolLevel parse(byte value)
    {
        for (BACnetProtocolLevel t : values())
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
