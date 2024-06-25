/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetSilencedState implements TypedBitSet.ValueGetter
{
    // @formatter:off
    unsilenced      (0),
    audible_silenced(1),
    visible_silenced(2),
    all_silenced    (3);
    // @formatter:on

    private final byte m_encoding;

    BACnetSilencedState(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetSilencedState parse(byte value)
    {
        for (BACnetSilencedState t : values())
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
