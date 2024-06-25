/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.hendrickson.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum WheelBalanceHealth implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Balanced           (0),
    OutOfBalance       (1),
    ExtremeOutOfBalance(2),
    Reserved3          (3),
    Reserved4          (4),
    NotSupported       (5),
    Error              (6),
    NotAvailable       (7);
    // @formatter:on

    private final byte m_encoding;

    WheelBalanceHealth(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static WheelBalanceHealth parse(byte value)
    {
        for (WheelBalanceHealth t : values())
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
