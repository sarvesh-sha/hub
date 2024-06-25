/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetAccumulatorStatus implements TypedBitSet.ValueGetter
{
    // @formatter:off
    normal   (0), 
    starting (1), 
    recovered(2), 
    abnormal (3), 
    failed   (4);
    // @formatter:on

    private final byte m_encoding;

    BACnetAccumulatorStatus(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetAccumulatorStatus parse(byte value)
    {
        for (BACnetAccumulatorStatus t : values())
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
