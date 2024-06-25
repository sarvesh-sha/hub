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

@JsonTypeName("Ipn:Obdii:Pgn:EngineFluidLevel_Pressure2")
@PgnMessageType(pgn = 65243, littleEndian = true, ignoreWhenReceived = false)
public class EngineFluidLevel_Pressure2 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Fuel Injection Control Pressure", units = EngineeringUnits.megapascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Fuel_Injection_Control_Pressure;

    @FieldModelDescription(description = "Engine Fuel 1 Injector Metering Rail 1 Pressure", units = EngineeringUnits.megapascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Fuel1_Injector_Metering_Rail1_Pressure;

    @FieldModelDescription(description = "Engine Fuel 1 Injector Timing Rail 1 Pressure", units = EngineeringUnits.megapascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Fuel1_Injector_Timing_Rail1_Pressure;

    @FieldModelDescription(description = "Engine Fuel 1 Injector Metering Rail 2 Pressure", units = EngineeringUnits.megapascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Fuel1_Injector_Metering_Rail2_Pressure;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "EngineFluidLevel_Pressure2";
    }
}