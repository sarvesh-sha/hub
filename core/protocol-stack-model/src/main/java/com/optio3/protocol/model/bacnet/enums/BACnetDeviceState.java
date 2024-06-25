/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetDeviceState implements TypedBitSet.ValueGetter
{
    // @formatter:off
    coldstart       (0),
    warmstart       (1),
    start_backup    (2),
    end_backup      (3),
    start_restore   (4),
    end_restore     (5),
    abort_restore   (6),
    activate_changes(7);
    // @formatter:on

    private final byte m_encoding;

    BACnetDeviceState(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetDeviceState parse(byte value)
    {
        for (BACnetDeviceState t : values())
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
