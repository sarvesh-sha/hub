/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetDoorValue implements TypedBitSet.ValueGetter
{
    // @formatter:off
    lock                 (0),
    unlock               (1),
    pulse_unlock         (2),
    extended_pulse_unlock(3);
    // @formatter:on

    private final byte m_encoding;

    BACnetDoorValue(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetDoorValue parse(byte value)
    {
        for (BACnetDoorValue t : values())
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
