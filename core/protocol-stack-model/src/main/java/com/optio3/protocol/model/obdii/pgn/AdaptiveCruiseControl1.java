/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.obdii.PgnMessageType;
import com.optio3.protocol.model.obdii.pgn.enums.PgnActivationMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnAdaptiveCruiseControlMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:AdaptiveCruiseControl1")
@PgnMessageType(pgn = 65135, littleEndian = true, ignoreWhenReceived = false)
public class AdaptiveCruiseControl1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Speed of forward vehicle", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 248)
    public float Speed_of_forward_vehicle;

    @FieldModelDescription(description = "Distance to forward vehicle", units = EngineeringUnits.meters, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Distance_to_forward_vehicle;

    @FieldModelDescription(description = "Adaptive Cruise Control Set Speed", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 248)
    public float Adaptive_Cruise_Control_Set_Speed;

    @FieldModelDescription(description = "Adaptive Cruise Control Mode", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 3, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnAdaptiveCruiseControlMode Adaptive_Cruise_Control_Mode;

    @FieldModelDescription(description = "Adaptive cruise control set distance mode", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 3, bitOffset = 3, scaling = { @SerializationScaling(scalingFactor = 8.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public Optional<Unsigned8> Adaptive_cruise_control_set_distance_mode;

    @FieldModelDescription(description = "Road curvature", units = EngineeringUnits.counts, debounceSeconds = 15, noValueMarker = -350.000000)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.0078125, postScalingOffset = -250.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Road_curvature;

    @FieldModelDescription(description = "ACC Target Detected", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 0)
    public PgnActivationMode ACC_Target_Detected;

    @FieldModelDescription(description = "ACC System Shutoff Warning", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 2)
    public PgnActivationMode ACC_System_Shutoff_Warning;

    @FieldModelDescription(description = "ACC Distance Alert Signal", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 4)
    public PgnActivationMode ACC_Distance_Alert_Signal;

    @FieldModelDescription(description = "Forward Collision Warning", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Forward_Collision_Warning;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "AdaptiveCruiseControl1";
    }
}