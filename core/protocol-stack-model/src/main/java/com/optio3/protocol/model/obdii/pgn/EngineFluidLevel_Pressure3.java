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

@JsonTypeName("Ipn:Obdii:Pgn:EngineFluidLevel_Pressure3")
@PgnMessageType(pgn = 64961, littleEndian = true, ignoreWhenReceived = false)
public class EngineFluidLevel_Pressure3 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Intake Valve Actuation System Oil Pressure", units = EngineeringUnits.megapascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Intake_Valve_Actuation_System_Oil_Pressure;

    @FieldModelDescription(description = "Engine Exhaust Gas Recirculation 1 Intake Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(scalingFactor = 2.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Exhaust_Gas_Recirculation1_Intake_Pressure;

    @FieldModelDescription(description = "Engine Exhaust Valve Actuation System Oil Pressure", units = EngineeringUnits.megapascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Exhaust_Valve_Actuation_System_Oil_Pressure;

    @FieldModelDescription(description = "Engine Exhaust Gas Recirculation 1 Outlet Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 6, width = 8, scaling = { @SerializationScaling(scalingFactor = 2.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Exhaust_Gas_Recirculation1_Outlet_Pressure;

    @FieldModelDescription(description = "Engine Throttle Valve 1 Differential Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.1, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Engine_Throttle_Valve1_Differential_Pressure;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "EngineFluidLevel_Pressure3";
    }
}