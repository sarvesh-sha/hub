/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.bluesky;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.ipn.enums.IpnAuxiliaryOutput;
import com.optio3.protocol.model.ipn.enums.IpnChargeEfficiency;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

//
// Byte 7: LSB of maximum battery voltage setpoint X10
// Byte 8: MSB of maximum battery voltage setpoint X10
// Byte 9: LSB of auxiliary for output off using voltage X10
// Byte 10: MSB of auxiliary for output off using voltage X10
// Byte 11: LSB of auxiliary for output on using voltage X10
// Byte 12: MSB of auxiliary for output on using voltage X10
// Byte 13: LSB of acceptance charge setpoint X10
// Byte 14: MSB of acceptance charge setpoint X10
// Byte 15: LSB of float charge setpoint X10
// Byte 16: MSB of float charge setpoint X10
// Byte 17: LSB of equalize charge setpoint X10
// Byte 18: MSB of equalize charge setpoint X10
// Byte 19: LSB of temperature compensation slope setpoint X100 in millivolts/°C/cell
// Byte 20: MSB of temperature compensation slope setpoint X100 in millivolts/°C/cell
// Byte 21: LSB of float current setpoint (amps per 100 amp-hours X10)
// Byte 22: MSB of float current setpoint (amps per 100 amp-hours X10)
// Byte 23: LSB of acceptance charge time setpoint X10
// Byte 24: MSB of acceptance charge time setpoint X10
// Byte 25: LSB of days between equalize
// Byte 26: MSB of days between equalize
// Byte 27: LSB of hours for an equalize cycle X10
// Byte 28: MSB of hours for an equalize cycle X10
// Byte 29: LSB of auxiliary output off using AHs
// Byte 30: MSB of auxiliary output off using AHs
// Byte 31: LSB of auxiliary output on using AHs
// Byte 32: MSB of auxiliary output on using AHs
// Byte 33: flags:
//          Bits 7, 6, 5, 4, 3, and 2: reserved
//          Bit 1: 1=charge efficiency fixed, 0=charge efficiency automatic
//          Bit 0: 1=auxillary output uses AHs, 0=auxillary output uses voltage
//

@JsonTypeName("Ipn:MasterSetpoints")
public class BlueSky_MasterSetpoints extends BaseBlueSkyObjectModel
{
    @FieldModelDescription(description = "Maximum Battery Voltage", units = EngineeringUnits.volts)
    @SerializationTag(number = 7, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float maximumBatteryVoltage;

    @FieldModelDescription(description = "Auxiliary Off Voltage", units = EngineeringUnits.volts)
    @SerializationTag(number = 9, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float auxiliaryOutputOffVoltage;

    @FieldModelDescription(description = "Auxiliary On Voltage", units = EngineeringUnits.volts)
    @SerializationTag(number = 11, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float auxiliaryOutputOnVoltage;

    @FieldModelDescription(description = "Acceptance Charge Voltage", units = EngineeringUnits.volts)
    @SerializationTag(number = 13, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float acceptanceCharge;

    @FieldModelDescription(description = "Float Charge Voltage", units = EngineeringUnits.volts)
    @SerializationTag(number = 15, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float floatCharge;

    @FieldModelDescription(description = "Equalizer Charge Voltage", units = EngineeringUnits.volts)
    @SerializationTag(number = 17, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float equalizeCharge;

    @FieldModelDescription(description = "T-Comp Slope mV/°C/Cell", units = EngineeringUnits.no_units)
    @SerializationTag(number = 19, width = 16, scaling = { @SerializationScaling(scalingFactor = -0.01) })
    public float temperatureCompensationSlope;

    @FieldModelDescription(description = "Float Current", units = EngineeringUnits.amperes)
    @SerializationTag(number = 21, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float floatCurrent;

    @FieldModelDescription(description = "Acceptance Charge Time", units = EngineeringUnits.minutes)
    @SerializationTag(number = 23, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float acceptanceChargeTime;

    @FieldModelDescription(description = "Days Between Equalize", units = EngineeringUnits.days)
    @SerializationTag(number = 25)
    public short daysBetweenEqualize;

    @FieldModelDescription(description = "Hours For Equalize Cycle", units = EngineeringUnits.hours)
    @SerializationTag(number = 27)
    public short hoursForEqualizeCycle;

    @FieldModelDescription(description = "Auxiliary Off amp-hours", units = EngineeringUnits.amperes)
    @SerializationTag(number = 29)
    public short auxiliaryOutputOffAh;

    @FieldModelDescription(description = "Auxiliary On amp-hours", units = EngineeringUnits.amperes)
    @SerializationTag(number = 31)
    public short auxiliaryOutputOnAh;

    @FieldModelDescription(description = "Charge Efficiency Mode", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 33, width = 1, bitOffset = 1)
    public IpnChargeEfficiency fixedEfficiency;

    @FieldModelDescription(description = "Auxiliary Output", units = EngineeringUnits.enumerated)
    @SerializationTag(number = 33, width = 1, bitOffset = 0)
    public IpnAuxiliaryOutput auxiliaryOutputUsesAHs;

    //--//

    @Override
    public String extractBaseId()
    {
        return "masterSetpoints";
    }

    @Override
    public boolean postDecodingValidation()
    {
        boolean ok = true;

        ok &= isAcceptableRange(maximumBatteryVoltage, 0, 50);
        ok &= isAcceptableRange(auxiliaryOutputOffVoltage, 0, 50);
        ok &= isAcceptableRange(auxiliaryOutputOnVoltage, 0, 50);

        return ok;
    }
}
