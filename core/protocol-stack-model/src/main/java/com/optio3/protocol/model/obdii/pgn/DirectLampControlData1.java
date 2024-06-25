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
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:DirectLampControlData1")
@PgnMessageType(pgn = 64773, littleEndian = true, ignoreWhenReceived = false)
public class DirectLampControlData1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Protect Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Protect_Lamp_Data;

    @FieldModelDescription(description = "Engine Amber Warning Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Amber_Warning_Lamp_Data;

    @FieldModelDescription(description = "Engine Red Stop Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Red_Stop_Lamp_Data;

    @FieldModelDescription(description = "OBD Malfunction Indicator Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode OBD_Malfunction_Indicator_Lamp_Data;

    @FieldModelDescription(description = "Engine Brake Active Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Brake_Active_Lamp_Data;

    @FieldModelDescription(description = "Compression Brake Enable Switch Indicator Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Compression_Brake_Enable_Switch_Indicator_Lamp_Data;

    @FieldModelDescription(description = "Engine Oil Pressure Low Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Oil_Pressure_Low_Lamp_Data;

    @FieldModelDescription(description = "Engine Coolant Temperature High Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Coolant_Temperature_High_Lamp_Data;

    @FieldModelDescription(description = "Engine Coolant Level Low Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Coolant_Level_Low_Lamp_Data;

    @FieldModelDescription(description = "Engine Idle Management Active Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Idle_Management_Active_Lamp_Data;

    @FieldModelDescription(description = "Engine Air Filter Restriction Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Air_Filter_Restriction_Lamp_Data;

    @FieldModelDescription(description = "Engine Fuel Filter Restricted Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Fuel_Filter_Restricted_Lamp_Data;

    @FieldModelDescription(description = "Engine Control Module 1 Ready for Use Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Control_Module1_Ready_for_Use_Lamp_Data;

    @FieldModelDescription(description = "Engine Control Module 2 Ready for Use Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Control_Module2_Ready_for_Use_Lamp_Data;

    @FieldModelDescription(description = "Engine Control Module 3 Ready for Use Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Control_Module3_Ready_for_Use_Lamp_Data;

    @FieldModelDescription(description = "Engine Speed High Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Speed_High_Lamp_Data;

    @FieldModelDescription(description = "Engine Speed Very High Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Speed_Very_High_Lamp_Data;

    @FieldModelDescription(description = "Vehicle Acceleration Rate Limit Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Vehicle_Acceleration_Rate_Limit_Lamp_Data;

    @FieldModelDescription(description = "Engine Stop-Start Automatic Stop Active Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Stop_Start_Automatic_Stop_Active_Lamp_Data;

    @FieldModelDescription(description = "Engine Stop-Start Automatic Start Failed Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Stop_Start_Automatic_Start_Failed_Lamp_Data;

    @FieldModelDescription(description = "Engine Stop-Start Enabled Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Stop_Start_Enabled_Lamp_Data;

    @FieldModelDescription(description = "Engine Wait To Start Lamp Data", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Wait_To_Start_Lamp_Data;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "DirectLampControlData1";
    }
}