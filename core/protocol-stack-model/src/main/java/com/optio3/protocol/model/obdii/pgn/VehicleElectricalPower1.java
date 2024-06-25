/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.obdii.PgnMessageType;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:VehicleElectricalPower1")
@PgnMessageType(pgn = 65271, littleEndian = true, ignoreWhenReceived = false)
public class VehicleElectricalPower1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "SLI Battery 1 Net Current", units = EngineeringUnits.amperes, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float SLI_Battery1_Net_Current;

    @FieldModelDescription(description = "Alternator Current", units = EngineeringUnits.amperes, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Alternator_Current;

    @FieldModelDescription(description = "Charging System Potential (Voltage)", units = EngineeringUnits.volts, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.05, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Charging_System_Potential_Voltage;

    @FieldModelDescription(description = "Battery Potential / Power Input 1", units = EngineeringUnits.volts, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.05, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Battery_Potential_Power_Input1;

    @FieldModelDescription(description = "Key Switch Battery Potential", units = EngineeringUnits.volts, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.05, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Key_Switch_Battery_Potential;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "VehicleElectricalPower1";
    }
}