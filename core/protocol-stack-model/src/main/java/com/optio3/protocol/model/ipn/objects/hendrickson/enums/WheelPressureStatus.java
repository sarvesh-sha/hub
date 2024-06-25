/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.hendrickson.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum WheelPressureStatus implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Ok                        (0),
    HighPressureWhileOperating(1),
    LowPressureWhileOperating (2),
    LowPressureWhileParked    (3),
    Reserved4                 (4),
    NotSupported              (5),
    Error                     (6),
    NotAvailable              (7);
    // @formatter:on

    private final byte m_encoding;

    WheelPressureStatus(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static WheelPressureStatus parse(byte value)
    {
        for (WheelPressureStatus t : values())
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
