/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetAuthorizationMode implements TypedBitSet.ValueGetter
{
    // @formatter:off
    authorize            (0),
    grant_active         (1),
    deny_all             (2),
    verification_required(3),
    authorization_delayed(4),
    none                 (5);
    // @formatter:on

    private final byte m_encoding;

    BACnetAuthorizationMode(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetAuthorizationMode parse(byte value)
    {
        for (BACnetAuthorizationMode t : values())
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
