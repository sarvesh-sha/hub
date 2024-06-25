/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.epsolar;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum EpSolarBatteryTemperatureStatus implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Normal  (0),
    OverTemp(1),
    LowTemp (2);
    // @formatter:on

    private final byte m_encoding;

    EpSolarBatteryTemperatureStatus(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static EpSolarBatteryTemperatureStatus parse(byte value)
    {
        for (EpSolarBatteryTemperatureStatus t : values())
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
