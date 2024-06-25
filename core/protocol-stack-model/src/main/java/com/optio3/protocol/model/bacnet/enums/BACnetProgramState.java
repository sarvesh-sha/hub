/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetProgramState implements TypedBitSet.ValueGetter
{
    // @formatter:off
    idle     (0), 
    loading  (1), 
    running  (2), 
    waiting  (3), 
    halted   (4), 
    unloading(5);
    // @formatter:on

    private final byte m_encoding;

    BACnetProgramState(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetProgramState parse(byte value)
    {
        for (BACnetProgramState t : values())
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
