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

@JsonTypeName("Ipn:Obdii:Pgn:ElectronicEngineController2")
@PgnMessageType(pgn = 61443, littleEndian = true, ignoreWhenReceived = false)
public class ElectronicEngineController2 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Accelerator Pedal 1 Low Idle Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Accelerator_Pedal1_Low_Idle_Switch;

    @FieldModelDescription(description = "Accelerator Pedal Kickdown Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Accelerator_Pedal_Kickdown_Switch;

    @FieldModelDescription(description = "Road Speed Limit Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Road_Speed_Limit_Status;

    @FieldModelDescription(description = "Accelerator Pedal 2 Low Idle Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Accelerator_Pedal2_Low_Idle_Switch;

    @FieldModelDescription(description = "Accelerator Pedal Position 1", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Accelerator_Pedal_Position1;

    @FieldModelDescription(description = "Engine Percent Load At Current Speed", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Percent_Load_At_Current_Speed;

    @FieldModelDescription(description = "Remote Accelerator Pedal Position", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Remote_Accelerator_Pedal_Position;

    @FieldModelDescription(description = "Accelerator Pedal 2 Position", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Accelerator_Pedal2_Position;

    @FieldModelDescription(description = "Vehicle Acceleration Rate Limit Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Vehicle_Acceleration_Rate_Limit_Status;

    @FieldModelDescription(description = "Momentary Engine Maximum Power Enable Feedback", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 2)
    public PgnActivationMode Momentary_Engine_Maximum_Power_Enable_Feedback;

    @FieldModelDescription(description = "DPF Thermal Management Active", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 4)
    public PgnActivationMode DPF_Thermal_Management_Active;

    @FieldModelDescription(description = "SCR Thermal Management Active", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 6)
    public PgnActivationMode SCR_Thermal_Management_Active;

    @FieldModelDescription(description = "Actual Maximum Available Engine - Percent Torque", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Actual_Maximum_Available_Engine_Percent_Torque;

    @FieldModelDescription(description = "Estimated Pumping - Percent Torque", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Estimated_Pumping_Percent_Torque;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "ElectronicEngineController2";
    }
}