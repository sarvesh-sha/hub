/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetSecurityPolicy implements TypedBitSet.ValueGetter
{
    // @formatter:off
    plain_non_trusted(0),
    plain_trusted    (1),
    signed_trusted   (2),
    encrypted_trusted(3);
    // @formatter:on

    private final byte m_encoding;

    BACnetSecurityPolicy(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetSecurityPolicy parse(byte value)
    {
        for (BACnetSecurityPolicy t : values())
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
