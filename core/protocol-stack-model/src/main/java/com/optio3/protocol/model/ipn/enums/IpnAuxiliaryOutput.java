/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum IpnAuxiliaryOutput implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Voltage(0),
    AHs    (1);
    // @formatter:on

    private final byte m_encoding;

    IpnAuxiliaryOutput(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static IpnAuxiliaryOutput parse(byte value)
    {
        for (IpnAuxiliaryOutput t : values())
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
