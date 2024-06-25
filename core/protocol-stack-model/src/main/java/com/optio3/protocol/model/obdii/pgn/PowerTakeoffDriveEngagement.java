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

@JsonTypeName("Ipn:Obdii:Pgn:PowerTakeoffDriveEngagement")
@PgnMessageType(pgn = 64932, littleEndian = true, ignoreWhenReceived = false)
public class PowerTakeoffDriveEngagement extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Enable Switch Ð Transfer case output shaft PTO", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Enable_Switch_Transfer_case_output_shaft_PTO;

    @FieldModelDescription(description = "Enable Switch Ð Transmission output shaft PTO", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Enable_Switch_Transmission_output_shaft_PTO;

    @FieldModelDescription(description = "Enable Switch Ð Transmission input shaft PTO 2", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Enable_Switch_Transmission_input_shaft_PTO2;

    @FieldModelDescription(description = "Enable Switch Ð Transmission input shaft PTO 1", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Enable_Switch_Transmission_input_shaft_PTO1;

    @FieldModelDescription(description = "Enable Switch - PTO Engine Flywheel", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Enable_Switch_PTO_Engine_Flywheel;

    @FieldModelDescription(description = "Enable Switch - PTO Engine Accessory Drive 1", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Enable_Switch_PTO_Engine_Accessory_Drive1;

    @FieldModelDescription(description = "Enable Switch - PTO Engine Accessory Drive 2", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Enable_Switch_PTO_Engine_Accessory_Drive2;

    @FieldModelDescription(description = "Engagement Consent Ð Transfer case output shaft PTO", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engagement_Consent_Transfer_case_output_shaft_PTO;

    @FieldModelDescription(description = "Engagement Consent Ð Transmission output shaft PTO", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engagement_Consent_Transmission_output_shaft_PTO;

    @FieldModelDescription(description = "Engagement Consent Ð Transmission input shaft PTO 2", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engagement_Consent_Transmission_input_shaft_PTO2;

    @FieldModelDescription(description = "Engagement Consent Ð Transmission input shaft PTO 1", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engagement_Consent_Transmission_input_shaft_PTO1;

    @FieldModelDescription(description = "Engagement Consent - PTO Engine Flywheel", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engagement_Consent_PTO_Engine_Flywheel;

    @FieldModelDescription(description = "Engagement Consent - PTO Engine Accessory Drive 1", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engagement_Consent_PTO_Engine_Accessory_Drive1;

    @FieldModelDescription(description = "Engagement Consent - PTO Engine Accessory Drive 2", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engagement_Consent_PTO_Engine_Accessory_Drive2;

    @FieldModelDescription(description = "Engagement Status Ð Transfer case output shaft PTO", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engagement_Status_Transfer_case_output_shaft_PTO;

    @FieldModelDescription(description = "Engagement Status Ð Transmission output shaft PTO", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engagement_Status_Transmission_output_shaft_PTO;

    @FieldModelDescription(description = "Engagement Status Ð Transmission input shaft PTO 2", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engagement_Status_Transmission_input_shaft_PTO2;

    @FieldModelDescription(description = "Engagement Status Ð Transmission input shaft PTO 1", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engagement_Status_Transmission_input_shaft_PTO1;

    @FieldModelDescription(description = "Engagement Status - PTO Engine Flywheel", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engagement_Status_PTO_Engine_Flywheel;

    @FieldModelDescription(description = "Engagement Status - PTO Engine Accessory Drive 1", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engagement_Status_PTO_Engine_Accessory_Drive1;

    @FieldModelDescription(description = "Engagement Status - PTO Engine Accessory Drive 2", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engagement_Status_PTO_Engine_Accessory_Drive2;

    @FieldModelDescription(description = "At least one PTO engaged", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode At_least_one_PTO_engaged;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "PowerTakeoffDriveEngagement";
    }
}