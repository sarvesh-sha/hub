/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.epsolar;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum EpSolarBatteryVoltageStatus implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Normal       (0),
    Over         (1),
    Under        (2),
    OverDischarge(3),
    Fault        (4);
    // @formatter:on

    private final byte m_encoding;

    EpSolarBatteryVoltageStatus(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static EpSolarBatteryVoltageStatus parse(byte value)
    {
        for (EpSolarBatteryVoltageStatus t : values())
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
