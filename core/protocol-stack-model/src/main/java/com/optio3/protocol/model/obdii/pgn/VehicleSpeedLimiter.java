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
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:VehicleSpeedLimiter")
@PgnMessageType(pgn = 64664, littleEndian = true, ignoreWhenReceived = false)
public class VehicleSpeedLimiter extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Soft Top Speed Limit Active", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Soft_Top_Speed_Limit_Active;

    @FieldModelDescription(description = "Soft Top Ratio Limit Exceeded", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Soft_Top_Ratio_Limit_Exceeded;

    @FieldModelDescription(description = "Soft Top Speed Limit Remaining Distance", units = EngineeringUnits.kilometers, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Soft_Top_Speed_Limit_Remaining_Distance;

    @FieldModelDescription(description = "Soft Top Speed Limit Reset Distance", units = EngineeringUnits.kilometers, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Soft_Top_Speed_Limit_Reset_Distance;

    @FieldModelDescription(description = "Soft Top Speed Limit Remaining Continuous Interval Distance", units = EngineeringUnits.kilometers, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 6, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Soft_Top_Speed_Limit_Remaining_Continuous_Interval_Distance;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "VehicleSpeedLimiter";
    }
}