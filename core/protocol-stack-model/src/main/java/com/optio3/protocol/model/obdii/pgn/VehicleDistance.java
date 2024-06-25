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

@JsonTypeName("Ipn:Obdii:Pgn:VehicleDistance")
@PgnMessageType(pgn = 65248, littleEndian = true, ignoreWhenReceived = false)
public class VehicleDistance extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Trip Distance", units = EngineeringUnits.kilometers, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 32, scaling = { @SerializationScaling(scalingFactor = 0.125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 2147483647)
    public float Trip_Distance;

    @FieldModelDescription(description = "Total Vehicle Distance", units = EngineeringUnits.kilometers, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 32, scaling = { @SerializationScaling(scalingFactor = 0.125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 2147483647)
    public float Total_Vehicle_Distance;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "VehicleDistance";
    }
}