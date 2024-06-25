/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetProgramRequest implements TypedBitSet.ValueGetter
{
    // @formatter:off
    ready  (0), 
    load   (1), 
    run    (2), 
    halt   (3), 
    restart(4), 
    unload (5);
    // @formatter:on

    private final byte m_encoding;

    BACnetProgramRequest(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetProgramRequest parse(byte value)
    {
        for (BACnetProgramRequest t : values())
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
