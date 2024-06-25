/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.epsolar;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum EpSolarLoadControlMode implements TypedBitSet.ValueGetter
{
    // @formatter:off
    ManualControl   (0),
    LightOnOff      (1),
    LightOnPlusTimer(2),
    TimingControl   (3);
    // @formatter:on

    private final byte m_encoding;

    EpSolarLoadControlMode(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static EpSolarLoadControlMode parse(byte value)
    {
        for (EpSolarLoadControlMode t : values())
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
