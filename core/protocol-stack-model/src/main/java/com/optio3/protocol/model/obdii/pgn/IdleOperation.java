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

@JsonTypeName("Ipn:Obdii:Pgn:IdleOperation")
@PgnMessageType(pgn = 65244, littleEndian = true, ignoreWhenReceived = false)
public class IdleOperation extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Total Idle Fuel Used", units = EngineeringUnits.liters, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 32, scaling = { @SerializationScaling(scalingFactor = 0.5, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 2147483647)
    public float Engine_Total_Idle_Fuel_Used;

    @FieldModelDescription(description = "Engine Total Idle Hours", units = EngineeringUnits.hours, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 32, scaling = { @SerializationScaling(scalingFactor = 0.05, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 2147483647)
    public float Engine_Total_Idle_Hours;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "IdleOperation";
    }
}