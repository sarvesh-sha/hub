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

@JsonTypeName("Ipn:Obdii:Pgn:VehiclePosition1")
@PgnMessageType(pgn = 65267, littleEndian = true, ignoreWhenReceived = false)
public class VehiclePosition1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Latitude", units = EngineeringUnits.degrees_angular, debounceSeconds = 15, noValueMarker = -310.000000)
    @SerializationTag(number = 1, width = 32, scaling = { @SerializationScaling(scalingFactor = 1.0E-7, postScalingOffset = -210.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 2147483647)
    public double Latitude;

    @FieldModelDescription(description = "Longitude", units = EngineeringUnits.degrees_angular, debounceSeconds = 15, noValueMarker = -310.000000)
    @SerializationTag(number = 5, width = 32, scaling = { @SerializationScaling(scalingFactor = 1.0E-7, postScalingOffset = -210.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 2147483647)
    public double Longitude;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "VehiclePosition1";
    }
}