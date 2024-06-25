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
import com.optio3.protocol.model.obdii.pgn.enums.PgnTransmissionService;
import com.optio3.protocol.model.obdii.pgn.enums.PgnTransmissionWarning;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:ElectronicTransmissionController7")
@PgnMessageType(pgn = 65098, littleEndian = true, ignoreWhenReceived = false)
public class ElectronicTransmissionController7 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Transmission Current Range Display Blank State", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 2, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 4.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public Optional<Unsigned8> Transmission_Current_Range_Display_Blank_State;

    @FieldModelDescription(description = "Transmission Service Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnTransmissionService Transmission_Service_Indicator;

    @FieldModelDescription(description = "Transmission Requested Range Display Blank State", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 4)
    public PgnActivationMode Transmission_Requested_Range_Display_Blank_State;

    @FieldModelDescription(description = "Transmission Requested Range Display Flash State", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 6)
    public PgnActivationMode Transmission_Requested_Range_Display_Flash_State;

    @FieldModelDescription(description = "Transmission Ready for Brake Release", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Ready_for_Brake_Release;

    @FieldModelDescription(description = "Active Shift Console Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Active_Shift_Console_Indicator;

    @FieldModelDescription(description = "Transmission Engine Crank Enable", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Engine_Crank_Enable;

    @FieldModelDescription(description = "Transmission Shift Inhibit Indicator", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 2, bitOffset = 6, scaling = { @SerializationScaling(scalingFactor = 4.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public Optional<Unsigned8> Transmission_Shift_Inhibit_Indicator;

    @FieldModelDescription(description = "Transmission Mode 4 Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Mode4_Indicator;

    @FieldModelDescription(description = "Transmission Mode 3 Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Mode3_Indicator;

    @FieldModelDescription(description = "Transmission Mode 2 Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Mode2_Indicator;

    @FieldModelDescription(description = "Transmission Mode 1 Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Mode1_Indicator;

    @FieldModelDescription(description = "Transmission Requested Gear Feedback", units = EngineeringUnits.counts, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 4, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Transmission_Requested_Gear_Feedback;

    @FieldModelDescription(description = "Transmission Mode 5 Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Mode5_Indicator;

    @FieldModelDescription(description = "Transmission Mode 6 Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Mode6_Indicator;

    @FieldModelDescription(description = "Transmission Mode 7 Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Mode7_Indicator;

    @FieldModelDescription(description = "Transmission Mode 8 Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Mode8_Indicator;

    @FieldModelDescription(description = "Transmission Reverse Gear Shift Inhibit Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Reverse_Gear_Shift_Inhibit_Status;

    @FieldModelDescription(description = "Transmission Warning Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnTransmissionWarning Transmission_Warning_Indicator;

    @FieldModelDescription(description = "Transmission Mode 9 Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Mode9_Indicator;

    @FieldModelDescription(description = "Transmission Mode 10 Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 6, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Mode10_Indicator;

    @FieldModelDescription(description = "Transmission Air Supply Pressure Indicator", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 3, bitOffset = 0, scaling = { @SerializationScaling(scalingFactor = 8.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public Optional<Unsigned8> Transmission_Air_Supply_Pressure_Indicator;

    @FieldModelDescription(description = "Transmission Auto-Neutral (Manual Return) State", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 3, bitOffset = 3, scaling = { @SerializationScaling(scalingFactor = 8.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public Optional<Unsigned8> Transmission_Auto_Neutral_Manual_Return_State;

    @FieldModelDescription(description = "Transmission Manual Mode Indicator", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 7, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Manual_Mode_Indicator;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "ElectronicTransmissionController7";
    }
}