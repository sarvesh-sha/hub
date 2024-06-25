/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetDoorStatus implements TypedBitSet.ValueGetter
{
    // @formatter:off
    closed        (0),
    opened        (1),
    unknown       (2),
    door_fault    (3),
    unused        (4),
    none          (5),
    closing       (6),
    opening       (7),
    safety_locked (8),
    limited_opened(9);
    // @formatter:on

    private final byte m_encoding;

    BACnetDoorStatus(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetDoorStatus parse(byte value)
    {
        for (BACnetDoorStatus t : values())
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
