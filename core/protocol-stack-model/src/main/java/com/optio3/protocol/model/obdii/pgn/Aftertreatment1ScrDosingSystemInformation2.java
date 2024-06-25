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
import com.optio3.protocol.model.obdii.pgn.enums.PgnDoserValueProtectionRequest;
import com.optio3.protocol.model.obdii.pgn.enums.PgnScrFeedbackControlStatus;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:Aftertreatment1ScrDosingSystemInformation2")
@PgnMessageType(pgn = 64833, littleEndian = true, ignoreWhenReceived = false)
public class Aftertreatment1ScrDosingSystemInformation2 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Aftertreatment 1 SCR Dosing Air Assist Absolute Pressure", units = EngineeringUnits.kilopascals, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(scalingFactor = 8.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_SCR_Dosing_Air_Assist_Absolute_Pressure;

    @FieldModelDescription(description = "Aftertreatment 1 SCR Dosing Air Assist Valve", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_SCR_Dosing_Air_Assist_Valve;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Doser 1 Temperature", units = EngineeringUnits.degrees_celsius, debounceSeconds = 15, noValueMarker = -140.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(postScalingOffset = -40.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Aftertreatment1_Diesel_Exhaust_Fluid_Doser1_Temperature;

    @FieldModelDescription(description = "Aftertreatment 1 SCR Doser Valve Exhaust Temperature Reduction Request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 3, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnDoserValueProtectionRequest Aftertreatment1_SCR_Doser_Valve_Exhaust_Temperature_Reduction_Request;

    @FieldModelDescription(description = "Aftertreatment 1 SCR Feedback Control Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 3, bitOffset = 3, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnScrFeedbackControlStatus Aftertreatment1_SCR_Feedback_Control_Status;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Line Heater 1 State", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Diesel_Exhaust_Fluid_Line_Heater1_State;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Line Heater 1 Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 5, bitOffset = 2, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Aftertreatment1_Diesel_Exhaust_Fluid_Line_Heater1_Preliminary_FMI;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Line Heater 2 State", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Diesel_Exhaust_Fluid_Line_Heater2_State;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Line Heater 2 Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 6, width = 5, bitOffset = 2, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Aftertreatment1_Diesel_Exhaust_Fluid_Line_Heater2_Preliminary_FMI;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Line Heater 3 State", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Diesel_Exhaust_Fluid_Line_Heater3_State;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Line Heater 3 Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 5, bitOffset = 2, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Aftertreatment1_Diesel_Exhaust_Fluid_Line_Heater3_Preliminary_FMI;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Line Heater 4 State", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 8, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Aftertreatment1_Diesel_Exhaust_Fluid_Line_Heater4_State;

    @FieldModelDescription(description = "Aftertreatment 1 Diesel Exhaust Fluid Line Heater 4 Preliminary FMI", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 8, width = 5, bitOffset = 2, scaling = { @SerializationScaling(scalingFactor = 0.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 31)
    public Optional<Unsigned8> Aftertreatment1_Diesel_Exhaust_Fluid_Line_Heater4_Preliminary_FMI;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "Aftertreatment1ScrDosingSystemInformation2";
    }
}