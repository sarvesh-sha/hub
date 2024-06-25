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
import com.optio3.protocol.model.obdii.pgn.enums.PgnExternalBraking;
import com.optio3.protocol.model.obdii.pgn.enums.PgnExternalBrakingStatus;
import com.optio3.protocol.model.obdii.pgn.enums.PgnHaltBrake;
import com.optio3.protocol.model.obdii.pgn.enums.PgnHillHolder;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:ElectronicBrakeController5")
@PgnMessageType(pgn = 64964, littleEndian = true, ignoreWhenReceived = false)
public class ElectronicBrakeController5 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Brake Temperature Warning", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Brake_Temperature_Warning;

    @FieldModelDescription(description = "Halt brake mode", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 3, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnHaltBrake Halt_brake_mode;

    @FieldModelDescription(description = "Hill holder mode", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 3, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnHillHolder Hill_holder_mode;

    @FieldModelDescription(description = "Foundation Brake Use", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Foundation_Brake_Use;

    @FieldModelDescription(description = "XBR System State", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 2)
    public PgnExternalBrakingStatus XBR_System_State;

    @FieldModelDescription(description = "XBR Active Control Mode", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 4, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnExternalBraking XBR_Active_Control_Mode;

    @FieldModelDescription(description = "XBR Acceleration Limit", units = EngineeringUnits.meters_per_second_per_second, debounceSeconds = 15, noValueMarker = -113.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.1, postScalingOffset = -12.5, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 250)
    public float XBR_Acceleration_Limit;

    @FieldModelDescription(description = "Parking Brake Actuator Fully Activated", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Parking_Brake_Actuator_Fully_Activated;

    @FieldModelDescription(description = "Emergency Braking Active", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Emergency_Braking_Active;

    @FieldModelDescription(description = "Railroad Mode", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Railroad_Mode;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "ElectronicBrakeController5";
    }
}