/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetLiftCarDriveStatus implements TypedBitSet.ValueGetter
{
    // @formatter:off
    unknown          (0),
    stationary       (1),
    braking          (2),
    accelerate       (3),
    decelerate       (4),
    rated_speed      (5),
    single_floor_jump(6),
    two_floor_jump   (7),
    three_floor_jump (8),
    multi_floor_jump (9);
    // @formatter:on

    private final byte m_encoding;

    BACnetLiftCarDriveStatus(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetLiftCarDriveStatus parse(byte value)
    {
        for (BACnetLiftCarDriveStatus t : values())
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
