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

@JsonTypeName("Ipn:Obdii:Pgn:IntakeExhaustConditions2")
@PgnMessageType(pgn = 64976, littleEndian = true, ignoreWhenReceived = false)
public class IntakeExhaustConditions2 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Air Filter 2 Differential Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.05, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Air_Filter2_Differential_Pressure;

    @FieldModelDescription(description = "Engine Air Filter 3 Differential Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.05, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Air_Filter3_Differential_Pressure;

    @FieldModelDescription(description = "Engine Air Filter 4 Differential Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.05, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Air_Filter4_Differential_Pressure;

    @FieldModelDescription(description = "Engine Intake Manifold #2 Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 8, scaling = { @SerializationScaling(scalingFactor = 2.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Intake_Manifold2_Pressure;

    @FieldModelDescription(description = "Engine Intake Manifold #1 Absolute Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 8, scaling = { @SerializationScaling(scalingFactor = 2.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Intake_Manifold1_Absolute_Pressure;

    @FieldModelDescription(description = "Engine Intake Manifold 1 Absolute Pressure (High Resolution)", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 6, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Intake_Manifold1_Absolute_Pressure_High_Resolution;

    @FieldModelDescription(description = "Engine Intake Manifold 2 Absolute Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(scalingFactor = 2.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Intake_Manifold2_Absolute_Pressure;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "IntakeExhaustConditions2";
    }
}