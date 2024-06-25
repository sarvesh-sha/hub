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

@JsonTypeName("Ipn:Obdii:Pgn:CruiseControlVehicleSpeedSetup")
@PgnMessageType(pgn = 65261, littleEndian = true, ignoreWhenReceived = false)
public class CruiseControlVehicleSpeedSetup extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Maximum Vehicle Speed Limit", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 248)
    public float Maximum_Vehicle_Speed_Limit;

    @FieldModelDescription(description = "Cruise Control High Set Limit Speed", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 248)
    public float Cruise_Control_High_Set_Limit_Speed;

    @FieldModelDescription(description = "Cruise Control Low Set Limit Speed", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 248)
    public float Cruise_Control_Low_Set_Limit_Speed;

    @FieldModelDescription(description = "Maximum Vehicle Speed Limit (High Resolution)", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 63896)
    public float Maximum_Vehicle_Speed_Limit_High_Resolution;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "CruiseControlVehicleSpeedSetup";
    }
}