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

@JsonTypeName("Ipn:Obdii:Pgn:TurbochargerInformation3")
@PgnMessageType(pgn = 65177, littleEndian = true, ignoreWhenReceived = false)
public class TurbochargerInformation3 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Turbocharger 1 Compressor Intake Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -350.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.0078125, postScalingOffset = -250.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Turbocharger1_Compressor_Intake_Pressure;

    @FieldModelDescription(description = "Engine Turbocharger 2 Compressor Intake Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -350.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.0078125, postScalingOffset = -250.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Turbocharger2_Compressor_Intake_Pressure;

    @FieldModelDescription(description = "Engine Turbocharger 3 Compressor Intake Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -350.000000)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.0078125, postScalingOffset = -250.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Turbocharger3_Compressor_Intake_Pressure;

    @FieldModelDescription(description = "Engine Turbocharger 4 Compressor Intake Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -350.000000)
    @SerializationTag(number = 7, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.0078125, postScalingOffset = -250.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Turbocharger4_Compressor_Intake_Pressure;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "TurbochargerInformation3";
    }
}