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

@JsonTypeName("Ipn:Obdii:Pgn:ElectronicEngineController7")
@PgnMessageType(pgn = 64916, littleEndian = true, ignoreWhenReceived = false)
public class ElectronicEngineController7 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Exhaust Gas Recirculation 1 Valve Position", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.0025, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Exhaust_Gas_Recirculation1_Valve_Position;

    @FieldModelDescription(description = "Engine Exhaust Gas Recirculation 1 Valve 2 Position", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.0025, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Exhaust_Gas_Recirculation1_Valve2_Position;

    @FieldModelDescription(description = "Engine Crankcase Breather Oil Separator Speed", units = EngineeringUnits.revolutions_per_minute, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Crankcase_Breather_Oil_Separator_Speed;

    @FieldModelDescription(description = "Engine Intake Manifold Commanded Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Intake_Manifold_Commanded_Pressure;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "ElectronicEngineController7";
    }
}