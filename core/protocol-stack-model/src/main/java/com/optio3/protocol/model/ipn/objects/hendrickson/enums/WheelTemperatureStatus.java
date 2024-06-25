/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.hendrickson.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum WheelTemperatureStatus implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Ok              (0),
    OverTemperature (1),
    UnderTemperature(2),
    Reserved3       (3),
    Reserved4       (4),
    NotSupported    (5),
    Error           (6),
    NotAvailable    (7);
    // @formatter:on

    private final byte m_encoding;

    WheelTemperatureStatus(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static WheelTemperatureStatus parse(byte value)
    {
        for (WheelTemperatureStatus t : values())
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
