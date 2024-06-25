/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetDoorAlarmState implements TypedBitSet.ValueGetter
{
    // @formatter:off
    normal            (0),
    alarm             (1),
    door_open_too_long(2),
    forced_open       (3),
    tamper            (4),
    door_fault        (5),
    lock_down         (6),
    free_access       (7),
    egress_open       (8);
    // @formatter:on

    private final byte m_encoding;

    BACnetDoorAlarmState(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetDoorAlarmState parse(byte value)
    {
        for (BACnetDoorAlarmState t : values())
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
