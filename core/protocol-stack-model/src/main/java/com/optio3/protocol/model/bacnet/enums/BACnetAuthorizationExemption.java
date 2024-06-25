/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetAuthorizationExemption implements TypedBitSet.ValueGetter
{
    // @formatter:off
    passback           (0),
    occupancy_check    (1),
    access_rights      (2),
    lockout            (3),
    deny               (4),
    verification       (5),
    authorization_delay(6);
    // @formatter:on

    private final byte m_encoding;

    BACnetAuthorizationExemption(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetAuthorizationExemption parse(byte value)
    {
        for (BACnetAuthorizationExemption t : values())
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
