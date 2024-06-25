/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnIsoTransportLayerType implements TypedBitSet.ValueGetter
{
    // @formatter:off
    Single     (0),
    First      (1),
    Consecutive(2),
    FlowControl(3);
    // @formatter:on

    private final byte m_encoding;

    PgnIsoTransportLayerType(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnIsoTransportLayerType parse(byte value)
    {
        for (PgnIsoTransportLayerType t : values())
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
