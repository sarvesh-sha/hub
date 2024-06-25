/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetDeviceStatus implements TypedBitSet.ValueGetter
{
    // @formatter:off
    operational          (0),
    operational_read_only(1),
    download_required    (2),
    download_in_progress (3),
    non_operational      (4),
    backup_in_progress   (5);
    // @formatter:on

    private final byte m_encoding;

    BACnetDeviceStatus(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetDeviceStatus parse(byte value)
    {
        for (BACnetDeviceStatus t : values())
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
