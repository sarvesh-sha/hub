/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetEventStateFilter implements TypedBitSet.ValueGetter
{
    // @formatter:off
    offnormal(0),
    fault    (1),
    normal   (2),
    all      (3),
    active   (4);
    // @formatter:on

    private final byte m_encoding;

    BACnetEventStateFilter(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetEventStateFilter parse(byte value)
    {
        for (BACnetEventStateFilter t : values())
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
