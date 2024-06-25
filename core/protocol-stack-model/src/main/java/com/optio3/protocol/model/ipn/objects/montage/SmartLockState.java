/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.montage;

import java.util.BitSet;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.optio3.cloud.model.IEnumDescription;
import com.optio3.serialization.TypedBitSet;

public final class SmartLockState extends TypedBitSet<SmartLockState.Values>
{
    @SuppressWarnings("PointlessArithmeticExpression")
    public enum Values implements ValueGetter,
                                  IEnumDescription
    {
        // @formatter:off
        UnlockedByMotor          ( 0*8+0, "Unlocked by motor"      , null),
        UnlockedByCylinder       ( 0*8+1, "Unlocked by cylinder"   , null),
        Locked                   ( 0*8+2, "Locked"                 , null),
        MechanicalJamming        ( 0*8+3, "Mechanical Jamming"     , null),
        CoverTampering           ( 0*8+4, "Cover Tampering"        , null),
        MagneticTampering        ( 0*8+5, "Magnetic Tampering"     , null),
        PinInPlace               ( 0*8+6, "Pin In Place"           , null),
        HandleInPlace            ( 0*8+7, "Handle in place"        , null),
        HighTemperature          ( 1*8+0, "High Temperature"       , null),
        LowTemperature           ( 1*8+1, "Low Temperature"        , null),
        HighVibration            ( 1*8+2, "High Vibration"         , null),
        UnlockingMotorTimeout    ( 1*8+3, "Unlocking Motor Timeout", null),
        LowBattery               ( 1*8+4, "Low Battery"            , null),
        LowLowBattery            ( 1*8+5, "Low Low Battery"        , null),
        NfcError                 ( 1*8+6, "NFC Error"              , null),
        I2CError                 ( 1*8+7, "I2C Error"              , null),
        NfcSuccessUnlocking      ( 2*8+0, "NFC Success Unlocking"  , null),
        NfcFailureUnlocking      ( 2*8+1, "NFC Failure Unlocking"  , null),
        BleSuccessUnlocking      ( 2*8+2, "BLE Success Unlocking"  , null),
        BleFailureUnlocking      ( 2*8+3, "BLE Failure Unlocking"  , null);
        // @formatter:on

        private final int    m_offset;
        private final String m_displayName;
        private final String m_description;

        Values(int offset,
               String displayName,
               String description)
        {
            m_offset      = (byte) offset;
            m_displayName = displayName;
            m_description = description;
        }

        @Override
        public int getEncodingValue()
        {
            return m_offset;
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

    public SmartLockState()
    {
        super(Values.class, Values.values());
    }

    public SmartLockState(BitSet bs)
    {
        super(Values.class, Values.values(), bs);
    }

    @JsonCreator
    public SmartLockState(List<String> inputs)
    {
        super(Values.class, Values.values(), inputs);
    }
}
