/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetEscalatorMode implements TypedBitSet.ValueGetter
{
    // @formatter:off
    unknown       (0),
    stop          (1),
    up            (2),
    down          (3),
    inspection    (4),
    out_of_service(5);
    // @formatter:on

    private final byte m_encoding;

    BACnetEscalatorMode(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetEscalatorMode parse(byte value)
    {
        for (BACnetEscalatorMode t : values())
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
