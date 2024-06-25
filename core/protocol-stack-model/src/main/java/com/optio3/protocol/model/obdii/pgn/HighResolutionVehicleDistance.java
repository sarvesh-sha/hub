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

@JsonTypeName("Ipn:Obdii:Pgn:HighResolutionVehicleDistance")
@PgnMessageType(pgn = 65217, littleEndian = true, ignoreWhenReceived = false)
public class HighResolutionVehicleDistance extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Total Vehicle Distance (High Resolution)", units = EngineeringUnits.meters, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 32, scaling = { @SerializationScaling(scalingFactor = 5.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 2147483647)
    public float Total_Vehicle_Distance_High_Resolution;

    @FieldModelDescription(description = "Trip Distance (High Resolution)", units = EngineeringUnits.meters, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 32, scaling = { @SerializationScaling(scalingFactor = 5.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 2147483647)
    public float Trip_Distance_High_Resolution;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "HighResolutionVehicleDistance";
    }
}