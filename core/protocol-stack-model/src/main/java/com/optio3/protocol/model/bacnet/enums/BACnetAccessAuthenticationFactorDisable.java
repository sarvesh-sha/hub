/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetAccessAuthenticationFactorDisable implements TypedBitSet.ValueGetter
{
    // @formatter:off
    none              (0),
    disabled          (1),
    disabled_lost     (2),
    disabled_stolen   (3),
    disabled_damaged  (4),
    disabled_destroyed(5);
    // @formatter:on

    private final byte m_encoding;

    BACnetAccessAuthenticationFactorDisable(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetAccessAuthenticationFactorDisable parse(byte value)
    {
        for (BACnetAccessAuthenticationFactorDisable t : values())
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
