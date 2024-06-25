/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.enums;

import com.optio3.cloud.model.IEnumDescription;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum Ipn_PalFinger_DisplayCode implements TypedBitSet.ValueGetter,
                                                 IEnumDescription
{
    // @formatter:off
    None               (0x00, "( ) No Code"              , null),
    ControlSwitchOff   (0x3F, "(0) Control Switch Off"   , null),
    ControlSwitchOn    (0x06, "(1) Control Switch On"    , null),
    Undervoltage       (0x5B, "(2) Undervoltage"         , null),
    TiltSwitch         (0x4F, "(3) Tilt Switch"          , null),
    TiltSensorArm      (0x66, "(4) Tilt Sensor Arm"      , null),
    TiltSensorHead     (0x6D, "(5) Tilt Sensor Head"     , null),
    WarnFix            (0x7D, "(6) Warn Fix"             , null),
    CabinControlSwitch (0x07, "(7) Cabin Control Switch" , null),
    GeneralFault       (0x7F, "(8) General Fault"        , null),
    VoltageV02Missing  (0x77, "(A) Voltage V02 Missing"  , null),
    ErrorDuringOpening (0x7C, "(b) Error During Opening" , null),
    ErrorDuringClosing (0x58, "(c) Error During Closing" , null),
    ErrorDuringLowering(0x5E, "(d) Error During Lowering", null),
    EmergencyProgram   (0x79, "(E) Emergency Program"    , null);
    // @formatter:on

    private final byte   m_encoding;
    private final String m_displayName;
    private final String m_description;

    Ipn_PalFinger_DisplayCode(int encoding,
                              String displayName,
                              String description)
    {
        m_encoding = (byte) encoding;
        m_displayName = displayName;
        m_description = description;
    }

    @HandlerForDecoding
    public static Ipn_PalFinger_DisplayCode parse(byte value)
    {
        for (Ipn_PalFinger_DisplayCode t : values())
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

    @Override
    public String getDisplayName()
    {
        return m_displayName;
    }

    @Override
    public String getDescription()
    {
        return m_description;
    }
}
