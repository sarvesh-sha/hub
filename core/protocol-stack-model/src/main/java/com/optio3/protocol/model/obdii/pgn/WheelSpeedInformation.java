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

@JsonTypeName("Ipn:Obdii:Pgn:WheelSpeedInformation")
@PgnMessageType(pgn = 65215, littleEndian = true, ignoreWhenReceived = false)
public class WheelSpeedInformation extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Front Axle Speed", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 63896)
    public float Front_Axle_Speed;

    @FieldModelDescription(description = "Relative Speed; Front Axle, Left Wheel", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -108.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.0625, postScalingOffset = -7.8125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 248)
    public float Relative_Speed_Front_Axle_Left_Wheel;

    @FieldModelDescription(description = "Relative Speed; Front Axle, Right Wheel", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -108.000000)
    @SerializationTag(number = 4, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.0625, postScalingOffset = -7.8125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 248)
    public float Relative_Speed_Front_Axle_Right_Wheel;

    @FieldModelDescription(description = "Relative Speed; Rear Axle #1, Left Wheel", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -108.000000)
    @SerializationTag(number = 5, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.0625, postScalingOffset = -7.8125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 248)
    public float Relative_Speed_Rear_Axle1_Left_Wheel;

    @FieldModelDescription(description = "Relative Speed; Rear Axle #1, Right Wheel", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -108.000000)
    @SerializationTag(number = 6, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.0625, postScalingOffset = -7.8125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 248)
    public float Relative_Speed_Rear_Axle1_Right_Wheel;

    @FieldModelDescription(description = "Relative Speed; Rear Axle #2, Left Wheel", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -108.000000)
    @SerializationTag(number = 7, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.0625, postScalingOffset = -7.8125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 248)
    public float Relative_Speed_Rear_Axle2_Left_Wheel;

    @FieldModelDescription(description = "Relative Speed; Rear Axle #2, Right Wheel", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -108.000000)
    @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.0625, postScalingOffset = -7.8125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 248)
    public float Relative_Speed_Rear_Axle2_Right_Wheel;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "WheelSpeedInformation";
    }
}