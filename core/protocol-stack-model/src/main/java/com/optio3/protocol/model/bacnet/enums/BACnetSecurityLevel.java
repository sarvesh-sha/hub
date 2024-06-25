/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetSecurityLevel implements TypedBitSet.ValueGetter
{
    // @formatter:off
    incapable           (0),
    plain               (1),
    signed              (2),
    encrypted           (3),
    signed_end_to_end   (4),
    encrypted_end_to_end(5);
    // @formatter:on

    private final byte m_encoding;

    BACnetSecurityLevel(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetSecurityLevel parse(byte value)
    {
        for (BACnetSecurityLevel t : values())
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
