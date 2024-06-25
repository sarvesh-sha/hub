/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.stealthpower;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum StealthPowerSystemStateForPEP implements TypedBitSet.ValueGetter
{
    // @formatter:off
    OEM_OFF         ( 0), // Conditions are not met for idle reduction (ignition not on, or not in park, or hood open)
    SP_OFF          ( 1), // Engine is running, and SP conditions are met (ignition is on, and in park, and hood closed)
    SP_LOW_BATT     ( 2), // Engine is running, the OEM battery went below the cutoff level, and the charge timer is now running (… FYI, the timer only decrements when the engine is running so it can be in this state across multiple engine runs if the driver turns off the engine before the charge is complete).
    SP_OUT_TEMP     ( 3), // Engine is running, the temperature is outside the high or low cutoff range, and the engine will not turn off until the temperature is back within the correct range for two minutes
    SP_ON           ( 4), // Engine is off, in Stealth Mode
    SP_KEY_INS      ( 5), // Engine is off, but we can’t start the engine (for safety reasons) because the driver must start the engine the first time the key is inserted (start of shift), or after the hood has been opened (mechanical work).
    ENG_STOP        ( 6), // Engine is being stopped
    ENG_START       ( 7), // Engine is being started
    SYS_PROTECT     ( 8), // The OEM battery voltage is below 11.1V, and a 5 minute counter has been started to cut off power to the MCU and Optio module by sending a 12V signal from the InvCtrl output.
    SHORELINE_CHARGE( 9), // Shoreline connected/inverter charging
    SP_OFF_INV      (10), // Engine is running/charging with the inverter on
    SP_ON_INV       (11), // Discharging the SP batteries with the inverter on
    SP_OFF_NOCHG    (12), // Engine running, not charging
    SP_OFF_NOCHG_INV(13), // Engine running, not charging
    SP_INV_ONLY     (14); // Key removed, inverter on
    // @formatter:on

    private final byte m_encoding;

    StealthPowerSystemStateForPEP(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static StealthPowerSystemStateForPEP parse(byte value)
    {
        for (StealthPowerSystemStateForPEP t : values())
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
