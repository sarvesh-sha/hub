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

@JsonTypeName("Ipn:Obdii:Pgn:VehicleHours")
@PgnMessageType(pgn = 65255, littleEndian = true, ignoreWhenReceived = false)
public class VehicleHours extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Total Vehicle Hours", units = EngineeringUnits.hours, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 32, scaling = { @SerializationScaling(scalingFactor = 0.05, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 2147483647)
    public float Total_Vehicle_Hours;

    @FieldModelDescription(description = "Total Power Takeoff Hours", units = EngineeringUnits.hours, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 32, scaling = { @SerializationScaling(scalingFactor = 0.05, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 2147483647)
    public float Total_Power_Takeoff_Hours;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "VehicleHours";
    }
}