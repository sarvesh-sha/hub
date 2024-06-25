/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.hendrickson.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum WheelConnectionStatus implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Ok          (0),
    NotFound    (1),
    Error       (2),
    NotAvailable(3);
    // @formatter:on

    private final byte m_encoding;

    WheelConnectionStatus(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static WheelConnectionStatus parse(byte value)
    {
        for (WheelConnectionStatus t : values())
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
