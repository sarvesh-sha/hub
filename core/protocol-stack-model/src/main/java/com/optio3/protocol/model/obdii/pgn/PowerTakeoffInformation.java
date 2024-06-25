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

@JsonTypeName("Ipn:Obdii:Pgn:PowerTakeoffInformation")
@PgnMessageType(pgn = 65264, littleEndian = true, ignoreWhenReceived = false)
public class PowerTakeoffInformation extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Power Takeoff Oil Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -140.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(postScalingOffset = -40.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Power_Takeoff_Oil_Temperature;

    @FieldModelDescription(description = "Power Takeoff Speed", units = EngineeringUnits.revolutions_per_minute, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Power_Takeoff_Speed;

    @FieldModelDescription(description = "Power Takeoff Set Speed", units = EngineeringUnits.revolutions_per_minute, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Power_Takeoff_Set_Speed;

    @FieldModelDescription(description = "Engine PTO Governor Enable Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_PTO_Governor_Enable_Switch;

    @FieldModelDescription(description = "Engine Remote PTO Governor Preprogrammed Speed Control Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Remote_PTO_Governor_Preprogrammed_Speed_Control_Switch;

    @FieldModelDescription(description = "Engine Remote PTO Governor Variable Speed Control Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Remote_PTO_Governor_Variable_Speed_Control_Switch;

    @FieldModelDescription(description = "Engine PTO Governor Set Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_PTO_Governor_Set_Switch;

    @FieldModelDescription(description = "Engine PTO Governor Coast/Decelerate Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_PTO_Governor_Coast_Decelerate_Switch;

    @FieldModelDescription(description = "Engine PTO Governor Resume Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_PTO_Governor_Resume_Switch;

    @FieldModelDescription(description = "Engine PTO Governor Accelerate Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_PTO_Governor_Accelerate_Switch;

    @FieldModelDescription(description = "Operator Engine PTO Governor Memory Select Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Operator_Engine_PTO_Governor_Memory_Select_Switch;

    @FieldModelDescription(description = "Remote PTO Governor Preprogrammed Speed Control Switch #2", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Remote_PTO_Governor_Preprogrammed_Speed_Control_Switch2;

    @FieldModelDescription(description = "Auxiliary Input Ignore Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Auxiliary_Input_Ignore_Switch;

    @FieldModelDescription(description = "Engine PTO Governor Disable Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_PTO_Governor_Disable_Switch;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "PowerTakeoffInformation";
    }
}