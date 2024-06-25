/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.hendrickson.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum WheelLeakStatus implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Ok                    (0),
    SlowLeakWhileParked   (1),
    SlowLeakWhileOperating(2),
    CriticalLeak          (3),
    Reserved4             (4),
    NotSupported          (5),
    Error                 (6),
    NotAvailable          (7);
    // @formatter:on

    private final byte m_encoding;

    WheelLeakStatus(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static WheelLeakStatus parse(byte value)
    {
        for (WheelLeakStatus t : values())
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
