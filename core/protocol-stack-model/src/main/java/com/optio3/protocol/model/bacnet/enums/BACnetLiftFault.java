/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum BACnetLiftFault implements TypedBitSet.ValueGetter
{
    // @formatter:off
    controller_fault                (0),
    drive_and_motor_fault           (1),
    governor_and_safety_gear_fault  (2),
    lift_shaft_device_fault         (3),
    power_supply_fault              (4),
    safety_interlock_fault          (5),
    door_closing_fault              (6),
    door_opening_fault              (7),
    car_stopped_outside_landing_zone(8),
    call_button_stuck               (9),
    start_failure                   (10),
    controller_supply_fault         (11),
    self_test_failure               (12),
    runtime_limit_exceeded          (13),
    position_lost                   (14),
    drive_temperature_exceeded      (15),
    load_measurement_fault          (16);
    // @formatter:on

    private final byte m_encoding;

    BACnetLiftFault(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static BACnetLiftFault parse(byte value)
    {
        for (BACnetLiftFault t : values())
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
