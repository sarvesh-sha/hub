/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.bluesky;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

//
// Lifetime Bat Dischg AH LSB
// Lifetime Bat Dischg AH Middle
// Lifetime Bat Dischg AH MSB
// Max Battery Volts LSB
// Max Battery Volts MSB
// Min Battery Volts LSB
// Min Battery Volts MSB
// Net Battery Current LSB 2s Comp
// Net Battery Current MSB 2s Comp
// Battery Capacity
// Battery AH SetPt LSB
// Battery AH SetPt MSB
// Charge Efficency SetPt
// Battery AH from Full LSB
// Battery AH from Full MSB
// Self Discharge SetPt
//

@JsonTypeName("Ipn:ProRemoteValues")
public class BlueSky_ProRemoteValues extends BaseBlueSkyObjectModel
{
    @FieldModelDescription(description = "Lifetime Amp-Hours", units = EngineeringUnits.ampere_seconds, pointClass = WellKnownPointClass.TotalDischarge, debounceSeconds = 15)
    @SerializationTag(number = 7, width = 24, bitOffset = 0, scaling = { @SerializationScaling(assumeUnsigned = true, scalingFactor = 3600) })
    public float lifeTimeBatteryDischargeAH;

    @FieldModelDescription(description = "Maximum Battery Voltage Detected", units = EngineeringUnits.volts, debounceSeconds = 15)
    @SerializationTag(number = 12, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float maxBatteryVolt;

    @FieldModelDescription(description = "Minimum Battery Voltage Detected", units = EngineeringUnits.volts, debounceSeconds = 15)
    @SerializationTag(number = 14, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float minBatteryVolt;

    @FieldModelDescription(description = "Net Battery Amps", units = EngineeringUnits.amperes, pointClass = WellKnownPointClass.LoadCurrent, debounceSeconds = 15)
    @SerializationTag(number = 16, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float netBatteryCurrent;

    @FieldModelDescription(description = "% Remaining Battery Capacity", units = EngineeringUnits.percent, debounceSeconds = 15)
    @SerializationTag(number = 18, width = 8, scaling = { @SerializationScaling(scalingFactor = 1.0) })
    public float batteryCapacity;

    @FieldModelDescription(description = "Battery Setpoint Amp-Hours", units = EngineeringUnits.ampere_seconds, debounceSeconds = 15)
    @SerializationTag(number = 19, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true, scalingFactor = 3600) })
    public float batterySetpointAH;

    @SerializationTag(number = 21, width = 8, scaling = { @SerializationScaling(scalingFactor = 1.0) })
    public float chargeEfficiencySetpoint;

    @FieldModelDescription(description = "Battery Amp-Hours from Full", units = EngineeringUnits.ampere_seconds, debounceSeconds = 15)
    @SerializationTag(number = 22, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true, scalingFactor = 3600) })
    public float batteryFromFullAH;

    @SerializationTag(number = 24)
    public byte selfDischargeSetpoint;

    //--//

    @Override
    public String extractBaseId()
    {
        return "proRemoteValues";
    }

    @Override
    public boolean postDecodingValidation()
    {
        boolean ok = true;

        ok &= isAcceptableRange(maxBatteryVolt, 0, 50);
        ok &= isAcceptableRange(minBatteryVolt, 0, 50);
        ok &= isAcceptableRange(netBatteryCurrent, -200, 200);
        ok &= isAcceptableRange(batteryCapacity, 0, 100);
        ok &= isAcceptableRange(batterySetpointAH, 0, 3600L * (1 << 16));

        return ok;
    }
}
