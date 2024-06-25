/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetTimerTransition implements TypedBitSet.ValueGetter
{
    // @formatter:off
    none              (0), 
    idle_to_running   (1), 
    running_to_idle   (2), 
    running_to_running(3), 
    running_to_expired(4), 
    forced_to_expired (5), 
    expired_to_idle   (6), 
    expired_to_running(7);
    // @formatter:on

    private final byte m_encoding;

    BACnetTimerTransition(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetTimerTransition parse(byte value)
    {
        for (BACnetTimerTransition t : values())
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
