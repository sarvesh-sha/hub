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

@JsonTypeName("Ipn:Obdii:Pgn:EngineGasFlowRate")
@PgnMessageType(pgn = 61450, littleEndian = true, ignoreWhenReceived = false)
public class EngineGasFlowRate extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Exhaust Gas Recirculation 1 Mass Flow Rate", units = EngineeringUnits.kilograms_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.05, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Exhaust_Gas_Recirculation1_Mass_Flow_Rate;

    @FieldModelDescription(description = "Engine Intake Air Mass Flow Rate", units = EngineeringUnits.kilograms_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.05, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Intake_Air_Mass_Flow_Rate;

    @FieldModelDescription(description = "Engine Exhaust Gas Recirculation 2 Mass Flow Rate", units = EngineeringUnits.kilograms_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.05, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Exhaust_Gas_Recirculation2_Mass_Flow_Rate;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "EngineGasFlowRate";
    }
}