/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetEscalatorFault implements TypedBitSet.ValueGetter
{
    // @formatter:off
    controller_fault          (0),
    drive_and_motor_fault     (1),
    mechanical_component_fault(2),
    overspeed_fault           (3),
    power_supply_fault        (4),
    safety_device_fault       (5),
    controller_supply_fault   (6),
    drive_temperature_exceeded(7),
    comb_plate_fault          (8);
    // @formatter:on

    private final byte m_encoding;

    BACnetEscalatorFault(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetEscalatorFault parse(byte value)
    {
        for (BACnetEscalatorFault t : values())
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
