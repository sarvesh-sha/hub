/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetEscalatorOperationDirection implements TypedBitSet.ValueGetter
{
    // @formatter:off
    unknown           (0),
    stopped           (1),
    up_rated_speed    (2),
    up_reduced_speed  (3),
    down_rated_speed  (4),
    down_reduced_speed(5);
    // @formatter:on

    private final byte m_encoding;

    BACnetEscalatorOperationDirection(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetEscalatorOperationDirection parse(byte value)
    {
        for (BACnetEscalatorOperationDirection t : values())
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
