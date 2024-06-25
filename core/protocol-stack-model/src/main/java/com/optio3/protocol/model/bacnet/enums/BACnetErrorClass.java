/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetErrorClass implements TypedBitSet.ValueGetter
{
    // @formatter:off
    device       (0), 
    object       (1), 
    property     (2), 
    resources    (3), 
    security     (4), 
    services     (5), 
    vt           (6), 
    communication(7);
    // @formatter:on

    private final byte m_encoding;

    BACnetErrorClass(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetErrorClass parse(byte value)
    {
        for (BACnetErrorClass t : values())
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
