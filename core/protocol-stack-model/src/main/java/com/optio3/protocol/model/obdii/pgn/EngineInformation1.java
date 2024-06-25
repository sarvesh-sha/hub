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

@JsonTypeName("Ipn:Obdii:Pgn:EngineInformation1")
@PgnMessageType(pgn = 65170, littleEndian = true, ignoreWhenReceived = false)
public class EngineInformation1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Oil Filter Intake Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(scalingFactor = 4.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Oil_Filter_Intake_Pressure;

    @FieldModelDescription(description = "Engine Exhaust Pressure 1", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -350.000000)
    @SerializationTag(number = 2, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.0078125, postScalingOffset = -250.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Exhaust_Pressure1;

    @FieldModelDescription(description = "Engine Fuel Rack Position", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Fuel_Rack_Position;

    @FieldModelDescription(description = "Engine Fuel System 1 Gas Mass Flow Rate", units = EngineeringUnits.kilograms_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.05, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Fuel_System1_Gas_Mass_Flow_Rate;

    @FieldModelDescription(description = "Instantaneous Estimated Brake Power", units = EngineeringUnits.kilowatts, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.5, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Instantaneous_Estimated_Brake_Power;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "EngineInformation1";
    }
}