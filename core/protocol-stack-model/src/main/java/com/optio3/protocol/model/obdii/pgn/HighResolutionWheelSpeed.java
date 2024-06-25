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

@JsonTypeName("Ipn:Obdii:Pgn:HighResolutionWheelSpeed")
@PgnMessageType(pgn = 65134, littleEndian = true, ignoreWhenReceived = false)
public class HighResolutionWheelSpeed extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Front Axle, Left Wheel Speed", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 63896)
    public float Front_Axle_Left_Wheel_Speed;

    @FieldModelDescription(description = "Front axle, right wheel speed", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 63896)
    public float Front_axle_right_wheel_speed;

    @FieldModelDescription(description = "Rear axle, left wheel speed", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 63896)
    public float Rear_axle_left_wheel_speed;

    @FieldModelDescription(description = "Rear axle, right wheel speed", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 63896)
    public float Rear_axle_right_wheel_speed;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "HighResolutionWheelSpeed";
    }
}