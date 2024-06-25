/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetRestartReason implements TypedBitSet.ValueGetter
{
    // @formatter:off
    unknown             (0),
    coldstart           (1),
    warmstart           (2),
    detected_power_lost (3),
    detected_powered_off(4),
    hardware_watchdog   (5),
    software_watchdog   (6),
    suspended           (7),
    activate_changes    (8);
    // @formatter:on

    private final byte m_encoding;

    BACnetRestartReason(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetRestartReason parse(byte value)
    {
        for (BACnetRestartReason t : values())
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
