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
import com.optio3.protocol.model.obdii.pgn.enums.PgnHeaterRequest;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:CabMessage1")
@PgnMessageType(pgn = 57344, littleEndian = true, ignoreWhenReceived = false)
public class CabMessage1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Fan 1 Requested Percent Speed", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Fan1_Requested_Percent_Speed;

    @FieldModelDescription(description = "Cab Interior Temperature Command", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -373.000000)
    @SerializationTag(number = 2, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.03125, postScalingOffset = -273.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Cab_Interior_Temperature_Command;

    @FieldModelDescription(description = "Auxiliary Heater Coolant Pump Request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 0)
    public PgnActivationMode Auxiliary_Heater_Coolant_Pump_Request;

    @FieldModelDescription(description = "Battery Main Switch Hold Request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 2)
    public PgnActivationMode Battery_Main_Switch_Hold_Request;

    @FieldModelDescription(description = "Operator Seat Direction Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Operator_Seat_Direction_Switch;

    @FieldModelDescription(description = "Seat Belt Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Seat_Belt_Switch;

    @FieldModelDescription(description = "Park Brake Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 0)
    public PgnActivationMode Park_Brake_Command;

    @FieldModelDescription(description = "Vehicle Limiting Speed Governor Decrement Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Vehicle_Limiting_Speed_Governor_Decrement_Switch;

    @FieldModelDescription(description = "Vehicle Limiting Speed Governor Increment Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Vehicle_Limiting_Speed_Governor_Increment_Switch;

    @FieldModelDescription(description = "Vehicle Limiting Speed Governor Enable Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Vehicle_Limiting_Speed_Governor_Enable_Switch;

    @FieldModelDescription(description = "Aftertreatment Regeneration Inhibit Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment_Regeneration_Inhibit_Switch;

    @FieldModelDescription(description = "Aftertreatment Regeneration Force Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment_Regeneration_Force_Switch;

    @FieldModelDescription(description = "Automatic Gear Shifting Enable Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Automatic_Gear_Shifting_Enable_Switch;

    @FieldModelDescription(description = "Engine Automatic Start Enable Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_Automatic_Start_Enable_Switch;

    @FieldModelDescription(description = "Auxiliary Heater Mode Request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 4, bitOffset = 0)
    public PgnHeaterRequest Auxiliary_Heater_Mode_Request;

    @FieldModelDescription(description = "Request Engine Zone Heating", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 4)
    public PgnActivationMode Request_Engine_Zone_Heating;

    @FieldModelDescription(description = "Request Cab Zone Heating", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 6)
    public PgnActivationMode Request_Cab_Zone_Heating;

    @FieldModelDescription(description = "Selected Maximum Vehicle Speed Limit", units = EngineeringUnits.counts, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Selected_Maximum_Vehicle_Speed_Limit;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "CabMessage1";
    }
}