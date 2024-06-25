/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetVTClass implements TypedBitSet.ValueGetter
{
    // @formatter:off
    default_terminal(0),
    ansi_x3_64      (1),
    dec_vt52        (2),
    dec_vt100       (3),
    dec_vt220       (4),
    hp_700_94       (5),
    ibm_3130        (6);
    // @formatter:on

    private final byte m_encoding;

    BACnetVTClass(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetVTClass parse(byte value)
    {
        for (BACnetVTClass t : values())
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
