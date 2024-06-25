/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetLiftCarMode implements TypedBitSet.ValueGetter
{
    // @formatter:off
    unknown             (0),
    normal              (1), // in service
    vip                 (2),
    homing              (3),
    parking             (4),
    attendant_control   (5),
    firefighter_control (6),
    emergency_power     (7),
    inspection          (8),
    cabinet_recall      (9),
    earthquake_operation(10),
    fire_operation      (11),
    out_of_service      (12),
    occupant_evacuation (13);
    // @formatter:on

    private final byte m_encoding;

    BACnetLiftCarMode(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetLiftCarMode parse(byte value)
    {
        for (BACnetLiftCarMode t : values())
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
