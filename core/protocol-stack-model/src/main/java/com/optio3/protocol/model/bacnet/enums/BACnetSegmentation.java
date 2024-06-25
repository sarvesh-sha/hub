/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetSegmentation implements TypedBitSet.ValueGetter
{
    // @formatter:off
    segmented_both    (0),
    segmented_transmit(1),
    segmented_receive (2),
    no_segmentation   (3);
    // @formatter:on

    private final byte m_encoding;

    BACnetSegmentation(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetSegmentation parse(byte value)
    {
        for (BACnetSegmentation t : values())
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
