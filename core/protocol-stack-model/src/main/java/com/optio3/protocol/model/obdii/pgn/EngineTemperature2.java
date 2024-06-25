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

@JsonTypeName("Ipn:Obdii:Pgn:EngineTemperature2")
@PgnMessageType(pgn = 65188, littleEndian = true, ignoreWhenReceived = false)
public class EngineTemperature2 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Oil Temperature 2", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -373.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.03125, postScalingOffset = -273.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Oil_Temperature2;

    @FieldModelDescription(description = "Engine ECU Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -373.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.03125, postScalingOffset = -273.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_ECU_Temperature;

    @FieldModelDescription(description = "Engine Exhaust Gas Recirculation 1 Differential Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -350.000000)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.0078125, postScalingOffset = -250.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Exhaust_Gas_Recirculation1_Differential_Pressure;

    @FieldModelDescription(description = "Engine Exhaust Gas Recirculation 1 Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -373.000000)
    @SerializationTag(number = 7, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.03125, postScalingOffset = -273.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Exhaust_Gas_Recirculation1_Temperature;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "EngineTemperature2";
    }
}