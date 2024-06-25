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

@JsonTypeName("Ipn:Obdii:Pgn:VehicleDynamicStabilityControl2")
@PgnMessageType(pgn = 61449, littleEndian = true, ignoreWhenReceived = false)
public class VehicleDynamicStabilityControl2 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Steering Wheel Angle", units = EngineeringUnits.radians, debounceSeconds = 15, noValueMarker = -132.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 9.765625E-4, postScalingOffset = -31.374, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Steering_Wheel_Angle;

    @FieldModelDescription(description = "Steering Wheel Turn Counter", units = EngineeringUnits.counts, debounceSeconds = 15, noValueMarker = -132.000000)
    @SerializationTag(number = 3, width = 6, bitOffset = 0, scaling = { @SerializationScaling(postScalingOffset = -32.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 63)
    public int Steering_Wheel_Turn_Counter;

    @FieldModelDescription(description = "Steering Wheel Angle Sensor Type", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Steering_Wheel_Angle_Sensor_Type;

    @FieldModelDescription(description = "Yaw Rate", units = EngineeringUnits.radians_per_second, debounceSeconds = 15, noValueMarker = -104.000000)
    @SerializationTag(number = 4, width = 16, scaling = { @SerializationScaling(scalingFactor = 1.2207031E-4, postScalingOffset = -3.92, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Yaw_Rate;

    @FieldModelDescription(description = "Lateral Acceleration", units = EngineeringUnits.meters_per_second_per_second, debounceSeconds = 15, noValueMarker = -116.000000)
    @SerializationTag(number = 6, width = 16, scaling = { @SerializationScaling(scalingFactor = 4.8828125E-4, postScalingOffset = -15.687, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Lateral_Acceleration;

    @FieldModelDescription(description = "Longitudinal Acceleration", units = EngineeringUnits.meters_per_second_per_second, debounceSeconds = 15, noValueMarker = -113.000000)
    @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.1, postScalingOffset = -12.5, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 250)
    public float Longitudinal_Acceleration;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "VehicleDynamicStabilityControl2";
    }
}