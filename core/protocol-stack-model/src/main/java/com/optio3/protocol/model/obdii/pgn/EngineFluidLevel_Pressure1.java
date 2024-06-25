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

@JsonTypeName("Ipn:Obdii:Pgn:EngineFluidLevel_Pressure1")
@PgnMessageType(pgn = 65263, littleEndian = true, ignoreWhenReceived = false)
public class EngineFluidLevel_Pressure1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Fuel Delivery Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(scalingFactor = 4.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Fuel_Delivery_Pressure;

    @FieldModelDescription(description = "Engine Extended Crankcase Blow-by Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.05, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Extended_Crankcase_Blow_by_Pressure;

    @FieldModelDescription(description = "Engine Oil Level", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Oil_Level;

    @FieldModelDescription(description = "Engine Oil Pressure 1", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 8, scaling = { @SerializationScaling(scalingFactor = 4.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Oil_Pressure1;

    @FieldModelDescription(description = "Engine Crankcase Pressure 1", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -350.000000)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.0078125, postScalingOffset = -250.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Crankcase_Pressure1;

    @FieldModelDescription(description = "Engine Coolant Pressure 1", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 8, scaling = { @SerializationScaling(scalingFactor = 2.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Coolant_Pressure1;

    @FieldModelDescription(description = "Engine Coolant Level 1", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Coolant_Level1;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "EngineFluidLevel_Pressure1";
    }
}