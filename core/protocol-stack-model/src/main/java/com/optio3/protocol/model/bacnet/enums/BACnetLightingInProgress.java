/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetLightingInProgress implements TypedBitSet.ValueGetter
{
    // @formatter:off
    idle          (0), 
    fade_active   (1), 
    ramp_active   (2), 
    not_controlled(3), 
    other         (4);
    // @formatter:on

    private final byte m_encoding;

    BACnetLightingInProgress(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetLightingInProgress parse(byte value)
    {
        for (BACnetLightingInProgress t : values())
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
