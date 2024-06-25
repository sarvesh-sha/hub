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
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnTorqueMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:ElectronicRetarderController1")
@PgnMessageType(pgn = 61440, littleEndian = true, ignoreWhenReceived = false)
public class ElectronicRetarderController1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Retarder Torque Mode", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 4, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnTorqueMode Retarder_Torque_Mode;

    @FieldModelDescription(description = "Retarder Enable - Brake Assist Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Retarder_Enable_Brake_Assist_Switch;

    @FieldModelDescription(description = "Retarder Enable - Shift Assist Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Retarder_Enable_Shift_Assist_Switch;

    @FieldModelDescription(description = "Actual Retarder - Percent Torque", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Actual_Retarder_Percent_Torque;

    @FieldModelDescription(description = "Intended Retarder Percent Torque", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Intended_Retarder_Percent_Torque;

    @FieldModelDescription(description = "Engine Coolant Load Increase", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Coolant_Load_Increase;

    @FieldModelDescription(description = "Retarder Requesting Brake Light", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Retarder_Requesting_Brake_Light;

    @FieldModelDescription(description = "Retarder Road Speed Limit Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Retarder_Road_Speed_Limit_Switch;

    @FieldModelDescription(description = "Retarder Road Speed Exceeded Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 6)
    public PgnActivationMode Retarder_Road_Speed_Exceeded_Status;

    @FieldModelDescription(description = "Source Address of Controlling Device for Retarder Control", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 8, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 255)
    public Optional<Unsigned8> Source_Address_of_Controlling_Device_for_Retarder_Control;

    @FieldModelDescription(description = "Drivers Demand Retarder -  Percent Torque", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 6, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Drivers_Demand_Retarder_Percent_Torque;

    @FieldModelDescription(description = "Retarder Selection, non-engine", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Retarder_Selection_non_engine;

    @FieldModelDescription(description = "Actual Maximum Available Retarder - Percent Torque", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Actual_Maximum_Available_Retarder_Percent_Torque;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "ElectronicRetarderController1";
    }
}