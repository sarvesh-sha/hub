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

@JsonTypeName("Ipn:Obdii:Pgn:FuelConsumptionLiquid1")
@PgnMessageType(pgn = 65257, littleEndian = true, ignoreWhenReceived = false)
public class FuelConsumptionLiquid1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Trip Fuel", units = EngineeringUnits.liters, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 32, scaling = { @SerializationScaling(scalingFactor = 0.5, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 2147483647)
    public float Engine_Trip_Fuel;

    @FieldModelDescription(description = "Engine Total Fuel Used", units = EngineeringUnits.liters, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 32, scaling = { @SerializationScaling(scalingFactor = 0.5, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 2147483647)
    public float Engine_Total_Fuel_Used;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "FuelConsumptionLiquid1";
    }
}