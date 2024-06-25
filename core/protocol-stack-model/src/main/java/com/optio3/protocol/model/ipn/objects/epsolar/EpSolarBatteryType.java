/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.epsolar;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum EpSolarBatteryType implements TypedBitSet.ValueGetter
{
    // @formatter:off
    UserDefined(0),
    Sealed     (1),
    Gel        (2),
    Flooded    (3);
    // @formatter:on

    private final byte m_encoding;

    EpSolarBatteryType(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static EpSolarBatteryType parse(byte value)
    {
        for (EpSolarBatteryType t : values())
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
