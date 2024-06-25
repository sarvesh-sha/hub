/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetBinaryLightingPV implements TypedBitSet.ValueGetter
{
    // @formatter:off
    off            (0), 
    on             (1), 
    warn           (2), 
    warn_off       (3), 
    warn_relinquish(4), 
    stop           (5);
    // @formatter:on

    private final byte m_encoding;

    BACnetBinaryLightingPV(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetBinaryLightingPV parse(byte value)
    {
        for (BACnetBinaryLightingPV t : values())
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
