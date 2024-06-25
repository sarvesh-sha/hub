/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.stealthpower;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum StealthPowerSystemStateForCAPMETRO implements TypedBitSet.ValueGetter
{
    // @formatter:off
    OEM_OFF         ( 0), // Conditions are not met for idle reduction (ignition not on, or not in park, or hood open)
    SP_OFF          ( 1), // Default engine running state/charging
    SP_LOW_BATT     ( 2), // SP batteries are low
    ESTART          ( 3), // Emergency/jump start
    SP_ON           ( 4), // Discharging the SP batteries
    SP_KEY_INS      ( 5), // Driver has not yet started the vehicle, everything off
    ENG_STOP        ( 6), // Engine is being stopped
    ENG_START       ( 7), // Engine is being started
    SYS_PROTECT     ( 8), // The system is turning itself off (including the Optio)
    SHORELINE_CHARGE( 9); // Shoreline connected/inverter charging
    // @formatter:on

    private final byte m_encoding;

    StealthPowerSystemStateForCAPMETRO(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static StealthPowerSystemStateForCAPMETRO parse(byte value)
    {
        for (StealthPowerSystemStateForCAPMETRO t : values())
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
