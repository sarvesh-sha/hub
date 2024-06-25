/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetAuthenticationStatus implements TypedBitSet.ValueGetter
{
    // @formatter:off
    not_ready                        (0),
    ready                            (1),
    disabled                         (2),
    waiting_for_authentication_factor(3),
    waiting_for_accompaniment        (4),
    waiting_for_verification         (5),
    in_progress                      (6);
    // @formatter:on

    private final byte m_encoding;

    BACnetAuthenticationStatus(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetAuthenticationStatus parse(byte value)
    {
        for (BACnetAuthenticationStatus t : values())
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
