/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.bluesky;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.ipn.enums.IpnAuxiliaryFET;
import com.optio3.protocol.model.ipn.enums.IpnAuxiliaryMode;
import com.optio3.protocol.model.ipn.enums.IpnChargeState;
import com.optio3.protocol.model.ipn.enums.IpnCompletedCycles;
import com.optio3.protocol.model.ipn.enums.IpnCycleKind;
import com.optio3.protocol.model.ipn.enums.IpnEqualizeHistory;
import com.optio3.protocol.model.ipn.enums.IpnPowerSource;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

//
// Byte 7: LSB of master’s input voltage X10
// Byte 8: MSB of master’s input voltage X10
// Byte 9: LSB of total input current from all power units X10
// Byte 10: MSB of total input current from all power units X10
// Byte 11: LSB of battery voltage as read by the master X10
// Byte 12: MSB of battery voltage as read by the master X10
// Byte 13: LSB of total output current from all power units X10
// Byte 14: MSB of total output current from all power units X10
// Byte 15: LSB of master’s auxiliary voltage X10
// Byte 16: MSB of master’s auxiliary voltage X10
// Byte 17: LSB of battery temperature as read by the master (degrees C + 60)
// Byte 18: MSB of battery temperature as read by the master (degrees C + 60)
// Byte 19: LSB of battery temperature sensor’s voltage X100
// Byte 20: MSB of battery temperature sensor’s voltage X100
// Byte 21: reserved
// Byte 22: reserved
// Byte 23: LSB of remaining equalize time in minutes
// Byte 24: MSB of remaining equalize time in minutes
// Byte 25: days since last full charge
// Byte 26: days since last equalize
// Byte 27: LSB of total charge amp-hours from all charge units
// Byte 28: MSB of total charge amp-hours from all charge units
// Byte 29: master’s charge state and flags
//         Bit 7: auxiliary mode (0=auxiliary charge, 1=load control)
//         Bit 6: state of auxiliary FET switch (0=off, 1=on)
//         Bit 5: power source, 1=DC, 0=PV modules
//         Bit 4: reserved
//         Bits 3, 2, 1, 0: charge state
//                0x0B: bulk, experimenting to improve MPPT in equalize
//                0x0A: bulk, normal MPPT in equalize
//                0x09: bulk, experimenting to improve MPPT (except equalize)
//                0x08: bulk, normal MPPT (except equalize)
//                0x07, 0x06, 0x05: reserved
//                0x04: current limit state
//                0x03: equalize state
//                0x02: float state
//                0x01: acceptance state
//                0x00: charge off state
// Byte 30: misc. flags:
//         Bit 7: completed charge/discharge cycles (0 = none, 1 = at least one)
//         Bit 6: 0 = end of charge/discharge cycle, 1 = presently in charge/discharge cycle
//         Bit 5: 0 = equalize did not occur this charge/discharge cycle
//                1 = equalize did occur this charge/discharge cycle
//         Bit 4: 1=slave to clear its charge amp-hours
//         Bits 3, 2, 1, 0: reserved

@JsonTypeName("Ipn:MasterValues")
public class BlueSky_MasterValues extends BaseBlueSkyObjectModel
{
    @FieldModelDescription(description = "Input Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.ArrayVoltage, pointClassPriority = 10, minimumDelta = 0.11,
                           debounceSeconds = 5)
    @SerializationTag(number = 7, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float inputVoltage;

    @FieldModelDescription(description = "Total Input Current", units = EngineeringUnits.amperes, pointClass = WellKnownPointClass.ArrayCurrent, pointClassPriority = 10, minimumDelta = 0.11,
                           debounceSeconds = 5)
    @SerializationTag(number = 9, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float totalInputCurrent;

    @FieldModelDescription(description = "Battery Voltage", units = EngineeringUnits.volts, pointClass = WellKnownPointClass.BatteryVoltage, pointClassPriority = 10, minimumDelta = 0.11,
                           debounceSeconds = 5)
    @SerializationTag(number = 11, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float batteryVoltage;

    @FieldModelDescription(description = "Total Output Current", units = EngineeringUnits.amperes, pointClass = WellKnownPointClass.BatteryCurrent, pointClassPriority = 10, minimumDelta = 0.11,
                           debounceSeconds = 5)
    @SerializationTag(number = 13, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float totalOutputCurrent;

    @FieldModelDescription(description = "Auxiliary Voltage", units = EngineeringUnits.volts, minimumDelta = 0.11, debounceSeconds = 5)
    @SerializationTag(number = 15, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float auxiliaryVoltage;

    @FieldModelDescription(description = "Battery Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 30)
    @SerializationTag(number = 17, width = 16, scaling = { @SerializationScaling(postScalingOffset = -60) })
    public float batteryTemperature;

    @FieldModelDescription(description = "Battery Temperature Sensor Voltage", units = EngineeringUnits.volts, minimumDelta = 0.05, debounceSeconds = 30)
    @SerializationTag(number = 19, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.01) })
    public float batteryTemperatureSensorVoltage;

    @JsonIgnore
    @SerializationTag(number = 21)
    public short reserved1;

    @FieldModelDescription(description = "Remaining equalize time", units = EngineeringUnits.minutes)
    @SerializationTag(number = 23)
    public short remainingEqualizeTime;

    @FieldModelDescription(description = "Days since last full charge", units = EngineeringUnits.days)
    @SerializationTag(number = 25)
    public Unsigned8 daysSinceLastFullCharge;

    @FieldModelDescription(description = "Days since last equalize", units = EngineeringUnits.days)
    @SerializationTag(number = 26)
    public Unsigned8 daysSinceLastEqualize;

    @FieldModelDescription(description = "Total Charge amp-hours", units = EngineeringUnits.ampere_seconds, pointClass = WellKnownPointClass.TotalCharge, pointClassPriority = 10)
    @SerializationTag(number = 27, width = 16, scaling = { @SerializationScaling(scalingFactor = 3600) })
    public float totalChargeAH;

    @FieldModelDescription(description = "Auxiliary Mode", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 29, width = 1, bitOffset = 7)
    public IpnAuxiliaryMode auxiliaryMode;

    @FieldModelDescription(description = "Auxiliary FET State", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 29, width = 1, bitOffset = 6)
    public IpnAuxiliaryFET auxiliaryFetState;

    @FieldModelDescription(description = "Power Source", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 29, width = 1, bitOffset = 5)
    public IpnPowerSource powerSource;

    @FieldModelDescription(description = "Charging State", units = EngineeringUnits.enumerated, pointClass = WellKnownPointClass.ChargingStatus, pointClassPriority = 10)
    @SerializationTag(number = 29, width = 4, bitOffset = 0)
    public IpnChargeState chargeState;

    @FieldModelDescription(description = "Completed Charge/Discharge Cycles", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 30, width = 1, bitOffset = 7)
    public IpnCompletedCycles completedCycles;

    @FieldModelDescription(description = "Cycle State", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 30, width = 1, bitOffset = 6)
    public IpnCycleKind inCycle;

    @FieldModelDescription(description = "Equalize Audit", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 30, width = 1, bitOffset = 5)
    public IpnEqualizeHistory equalizeInThisCycle;

    @SerializationTag(number = 30, width = 1, bitOffset = 4)
    public boolean clearChargeAH; // 1=slave to clear its charge amp-hours

    //--//

    @Override
    public String extractBaseId()
    {
        return "masterValues";
    }

    @Override
    public boolean postDecodingValidation()
    {
        boolean ok = true;

        ok &= isAcceptableRange(inputVoltage, 0, 50);
        ok &= isAcceptableRange(totalInputCurrent, 0, 200);
        ok &= isAcceptableRange(batteryVoltage, 1, 50);
        ok &= isAcceptableRange(totalOutputCurrent, 0, 200);
        ok &= isAcceptableRange(totalChargeAH, 0, 3600 * 65536);

        auxiliaryVoltage = capToAcceptableRange(auxiliaryVoltage, 0, 50);
        batteryTemperature = capToAcceptableRange(batteryTemperature, -60, 150);

        return ok;
    }
}
