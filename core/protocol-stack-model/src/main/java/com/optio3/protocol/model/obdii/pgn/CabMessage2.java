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
import com.optio3.protocol.model.obdii.pgn.enums.PgnActivationMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:CabMessage2")
@PgnMessageType(pgn = 34048, littleEndian = true, ignoreWhenReceived = false)
public class CabMessage2 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Fan 2 Requested Percent Speed", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Fan2_Requested_Percent_Speed;

    @FieldModelDescription(description = "Performance Bias Selection", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Performance_Bias_Selection;

    @FieldModelDescription(description = "SCR Operator Inducement Override Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode SCR_Operator_Inducement_Override_Switch;

    @FieldModelDescription(description = "Heat Exchanger Debris Purge Inhibit Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Heat_Exchanger_Debris_Purge_Inhibit_Switch;

    @FieldModelDescription(description = "Heat Exchanger Debris Purge Force Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Heat_Exchanger_Debris_Purge_Force_Switch;

    @FieldModelDescription(description = "Predictive Cruise Control Enable Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Predictive_Cruise_Control_Enable_Switch;

    @FieldModelDescription(description = "Predictive Cruise Control Deactivation Request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Predictive_Cruise_Control_Deactivation_Request;

    @FieldModelDescription(description = "Engine Stop-Start Disable Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 2)
    public PgnActivationMode Engine_Stop_Start_Disable_Command;

    @FieldModelDescription(description = "Elevated Engine Speed Allowed Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Elevated_Engine_Speed_Allowed_Switch;

    @FieldModelDescription(description = "Aftertreatment Regeneration Engine Speed Allowed Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment_Regeneration_Engine_Speed_Allowed_Switch;

    @FieldModelDescription(description = "Predictive Cruise Control Maximum Positive Offset", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 248)
    public float Predictive_Cruise_Control_Maximum_Positive_Offset;

    @FieldModelDescription(description = "Predictive Cruise Control Maximum Negative Offset", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -132.000000)
    @SerializationTag(number = 6, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.125, postScalingOffset = -31.25, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 248)
    public float Predictive_Cruise_Control_Maximum_Negative_Offset;

    @FieldModelDescription(description = "Transmission Auto-Neutral (Manual Return) Enable Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Auto_Neutral_Manual_Return_Enable_Switch;

    @FieldModelDescription(description = "Aftertreatment System Enable Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 2)
    public PgnActivationMode Aftertreatment_System_Enable_Command;

    @FieldModelDescription(description = "Active Shift Console Request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 4)
    public PgnActivationMode Active_Shift_Console_Request;

    @FieldModelDescription(description = "Engine Idle Management Pending Event Override", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Idle_Management_Pending_Event_Override;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "CabMessage2";
    }
}