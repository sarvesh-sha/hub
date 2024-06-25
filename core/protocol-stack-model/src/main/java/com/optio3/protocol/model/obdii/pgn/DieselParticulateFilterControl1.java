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
import com.optio3.protocol.model.obdii.pgn.enums.PgnParticulateFilterForcedExecution;
import com.optio3.protocol.model.obdii.pgn.enums.PgnParticulateFilterRegeneration;
import com.optio3.protocol.model.obdii.pgn.enums.PgnParticulateFilterRegenerationState;
import com.optio3.protocol.model.obdii.pgn.enums.PgnPurging;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnWarningStatus;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:DieselParticulateFilterControl1")
@PgnMessageType(pgn = 64892, littleEndian = true, ignoreWhenReceived = false)
public class DieselParticulateFilterControl1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Diesel Particulate Filter Lamp Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 3, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnWarningStatus Diesel_Particulate_Filter_Lamp_Command;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Availability Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 3, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Availability_Status;

    @FieldModelDescription(description = "Aftertreatment Diesel Particulate Filter Passive Regeneration Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment_Diesel_Particulate_Filter_Passive_Regeneration_Status;

    @FieldModelDescription(description = "Aftertreatment Diesel Particulate Filter Active Regeneration Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 2)
    public PgnParticulateFilterRegenerationState Aftertreatment_Diesel_Particulate_Filter_Active_Regeneration_Status;

    @FieldModelDescription(description = "Aftertreatment Diesel Particulate Filter Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 3, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnParticulateFilterRegeneration Aftertreatment_Diesel_Particulate_Filter_Status;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Status;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to Inhibit Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_Inhibit_Switch;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to Clutch Disengaged", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_Clutch_Disengaged;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to Service Brake Active", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_Service_Brake_Active;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to PTO Active", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_PTO_Active;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to Accelerator Pedal Off Idle", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_Accelerator_Pedal_Off_Idle;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to Out of Neutral", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_Out_of_Neutral;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to Vehicle Speed Above Allowed Speed", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_Vehicle_Speed_Above_Allowed_Speed;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to Parking Brake Not Set", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_Parking_Brake_Not_Set;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to Low Exhaust Temperature", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_Low_Exhaust_Temperature;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to System Fault Active", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_System_Fault_Active;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to System Timeout", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_System_Timeout;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to Temporary System Lockout", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_Temporary_System_Lockout;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to Permanent System Lockout", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_Permanent_System_Lockout;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to Engine Not Warmed Up", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_Engine_Not_Warmed_Up;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to Vehicle Speed Below Allowed Speed", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_Vehicle_Speed_Below_Allowed_Speed;

    @FieldModelDescription(description = "Diesel Particulate Filter Automatic Active Regeneration Initiation Configuration", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Automatic_Active_Regeneration_Initiation_Configuration;

    @FieldModelDescription(description = "Exhaust System High Temperature Lamp Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 3, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnWarningStatus Exhaust_System_High_Temperature_Lamp_Command;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Forced Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 3, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnParticulateFilterForcedExecution Diesel_Particulate_Filter_Active_Regeneration_Forced_Status;

    @FieldModelDescription(description = "Hydrocarbon Doser Purging Enable", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnPurging Hydrocarbon_Doser_Purging_Enable;

    @FieldModelDescription(description = "Diesel Particulate Filter Active Regeneration Inhibited Due to Low Exhaust Pressure", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Diesel_Particulate_Filter_Active_Regeneration_Inhibited_Due_to_Low_Exhaust_Pressure;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Particulate Filter Conditions Not Met for Active Regeneration", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Diesel_Particulate_Filter_Conditions_Not_Met_for_Active_Regeneration;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "DieselParticulateFilterControl1";
    }
}