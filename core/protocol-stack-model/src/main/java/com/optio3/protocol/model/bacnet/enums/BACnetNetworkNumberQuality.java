/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetNetworkNumberQuality implements TypedBitSet.ValueGetter
{
    // @formatter:off
    unknown           (0),
    learned           (1),
    learned_configured(2),
    configured        (3);
    // @formatter:on

    private final byte m_encoding;

    BACnetNetworkNumberQuality(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetNetworkNumberQuality parse(byte value)
    {
        for (BACnetNetworkNumberQuality t : values())
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
