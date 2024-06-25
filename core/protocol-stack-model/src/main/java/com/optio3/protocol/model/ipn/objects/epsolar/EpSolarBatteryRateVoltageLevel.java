/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.epsolar;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum EpSolarBatteryRateVoltageLevel implements TypedBitSet.ValueGetter
{
    // @formatter:off
    AutoRecognize(0),
    Rated_12V    (1),
    Rated_24V    (2),
    Rated_36V    (3),
    Rated_48V    (4),
    Rated_60V    (5),
    Rated_110V   (6),
    Rated_120V   (7),
    Rated_220V   (8),
    Rated_240V   (9);
    // @formatter:on

    private final byte m_encoding;

    EpSolarBatteryRateVoltageLevel(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static EpSolarBatteryRateVoltageLevel parse(byte value)
    {
        for (EpSolarBatteryRateVoltageLevel t : values())
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
