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

@JsonTypeName("Ipn:Obdii:Pgn:EngineTemperature1")
@PgnMessageType(pgn = 65262, littleEndian = true, ignoreWhenReceived = false)
public class EngineTemperature1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Coolant Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -140.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(postScalingOffset = -40.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Coolant_Temperature;

    @FieldModelDescription(description = "Engine Fuel 1 Temperature 1", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -140.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(postScalingOffset = -40.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Fuel1_Temperature1;

    @FieldModelDescription(description = "Engine Oil Temperature 1", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -373.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.03125, postScalingOffset = -273.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Oil_Temperature1;

    @FieldModelDescription(description = "Engine Turbocharger 1 Oil Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -373.000000)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.03125, postScalingOffset = -273.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Turbocharger1_Oil_Temperature;

    @FieldModelDescription(description = "Engine Intercooler Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -140.000000)
    @SerializationTag(number = 7, width = 8, scaling = { @SerializationScaling(postScalingOffset = -40.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Intercooler_Temperature;

    @FieldModelDescription(description = "Engine Charge Air Cooler Thermostat Opening", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Charge_Air_Cooler_Thermostat_Opening;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "EngineTemperature1";
    }
}