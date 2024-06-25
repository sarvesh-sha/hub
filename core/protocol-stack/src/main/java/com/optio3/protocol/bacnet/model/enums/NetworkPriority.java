/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;

public enum NetworkPriority
{
    // @formatter:off
    LifeSafety       (0b11),
    CriticalEquipment(0b10),
    Urgent           (0b01),
    Normal           (0b00);
    // @formatter:on

    private final byte m_encoding;

    NetworkPriority(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static NetworkPriority parse(byte value)
    {
        for (NetworkPriority t : values())
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
}
