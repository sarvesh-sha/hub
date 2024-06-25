/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetLightingOperation implements TypedBitSet.ValueGetter
{
    // @formatter:off
    none           (0),
    fade_to        (1),
    ramp_to        (2),
    step_up        (3),
    step_down      (4),
    step_on        (5),
    step_off       (6),
    warn           (7),
    warn_off       (8),
    warn_relinquish(9),
    stop           (10);
    // @formatter:on

    private final byte m_encoding;

    BACnetLightingOperation(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetLightingOperation parse(byte value)
    {
        for (BACnetLightingOperation t : values())
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
