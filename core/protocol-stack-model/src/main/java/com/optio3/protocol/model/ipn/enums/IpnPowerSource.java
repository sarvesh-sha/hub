/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum IpnPowerSource implements TypedBitSet.ValueGetter
{
    // @formatter:off
    PV(0),
    DC(1);
    // @formatter:on

    private final byte m_encoding;

    IpnPowerSource(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static IpnPowerSource parse(byte value)
    {
        for (IpnPowerSource t : values())
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
