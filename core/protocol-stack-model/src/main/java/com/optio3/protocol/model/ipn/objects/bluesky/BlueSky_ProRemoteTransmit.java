/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.bluesky;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

//
// EnaFlags
// Max Battery Volts SetPt LSB
// Max Battery Volts SetPt MSB
// Min Battery Volts SetPt LSB
// Min Battery Volts SetPt MSB
// Min Battery AH SetPt LSB
// Min Battery AH SetPt MSB
// Charge Efficency SetPt
// Shelf Discharge SetPt
//

@JsonTypeName("Ipn:ProRemoteTransmit")
public class BlueSky_ProRemoteTransmit extends BaseBlueSkyObjectModel
{
    @SerializationTag(number = 7)
    public byte enaFlags;

    @FieldModelDescription(description = "Maximum Battery Voltage Setpoint", units = EngineeringUnits.volts, debounceSeconds = 15)
    @SerializationTag(number = 8, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float maxBatteryVolt;

    @FieldModelDescription(description = "Maximum Battery Voltage Setpoint", units = EngineeringUnits.volts, debounceSeconds = 15)
    @SerializationTag(number = 10, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1) })
    public float minBatteryVolt;

    @FieldModelDescription(description = "Minimum Battery Setpoint Amp-Hours", units = EngineeringUnits.ampere_seconds, debounceSeconds = 15)
    @SerializationTag(number = 12, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true, scalingFactor = 3600) })
    public float minBatterySetpointAH;

    @FieldModelDescription(description = "Charge Efficiency % Setpoint", units = EngineeringUnits.percent, debounceSeconds = 15)
    @SerializationTag(number = 14, width = 8, scaling = { @SerializationScaling(scalingFactor = 1.0) })
    public float chargeEfficiencySetpoint;

    @FieldModelDescription(description = "Self-Discharge % per month Setpoint", units = EngineeringUnits.percent, debounceSeconds = 15)
    @SerializationTag(number = 15, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.01) })
    public float selfDischargeSetpoint;

    //--//

    @Override
    public String extractBaseId()
    {
        return "proRemoteTransmit";
    }

    @Override
    public boolean postDecodingValidation()
    {
        boolean ok = true;

        ok &= isAcceptableRange(maxBatteryVolt, 0, 50);
        ok &= isAcceptableRange(minBatteryVolt, 0, 50);
        ok &= isAcceptableRange(minBatterySetpointAH, 0, 3600 * 65536);

        return ok;
    }
}
