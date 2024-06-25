/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.stealthpower;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum StealthPowerSystemStateForMTA implements TypedBitSet.ValueGetter
{
    // @formatter:off
    OEM_OFF    (0), // Conditions are not met for idle reduction (ignition not on, or not in park, or hood open)
    SP_OFF     (1), // Engine is running, and SP conditions are met (ignition is on, and in park, and hood closed)
    SP_LOW_BATT(2), // Engine is running, the OEM battery went below the cutoff level, and the charge timer is now running (… FYI, the timer only decrements when the engine is running so it can be in this state across multiple engine runs if the driver turns off the engine before the charge is complete).
    SP_OUT_TEMP(3), // Engine is running, the temperature is outside the high or low cutoff range, and the engine will not turn off until the temperature is back within the correct range for two minutes
    SP_ON      (4), // Engine is off, in Stealth Mode
    SP_KEY_INS (5), // Engine is off, but we can’t start the engine (for safety reasons) because the driver must start the engine the first time the key is inserted (start of shift), or after the hood has been opened (mechanical work).
    ENG_STOP   (6), // Engine is being stopped
    ENG_START  (7), // Engine is being started
    SYS_PROTECT(8), // The OEM battery voltage is below 11.1V, and a 5 minute counter has been started to cut off power to the MCU and Optio module by sending a 12V signal from the InvCtrl output.
    //
    // Legacy values, to avoid breaking the time series
    RELAYS_OFF  (1000 + 0),
    RELAYS_STOP (1000 + 1),
    RELAYS_START(1000 + 2);
    // @formatter:on

    private final byte m_encoding;

    StealthPowerSystemStateForMTA(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static StealthPowerSystemStateForMTA parse(byte value)
    {
        for (StealthPowerSystemStateForMTA t : values())
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