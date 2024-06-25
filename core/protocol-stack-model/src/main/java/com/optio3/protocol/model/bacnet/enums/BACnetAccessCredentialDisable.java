/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetAccessCredentialDisable implements TypedBitSet.ValueGetter
{
    // @formatter:off
    none           (0),
    disable        (1),
    disable_manual (2),
    disable_lockout(3);
    // @formatter:on

    private final byte m_encoding;

    BACnetAccessCredentialDisable(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetAccessCredentialDisable parse(byte value)
    {
        for (BACnetAccessCredentialDisable t : values())
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
