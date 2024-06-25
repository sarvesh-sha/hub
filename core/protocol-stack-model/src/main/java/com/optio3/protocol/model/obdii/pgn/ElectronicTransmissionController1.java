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
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:ElectronicTransmissionController1")
@PgnMessageType(pgn = 61442, littleEndian = true, ignoreWhenReceived = false)
public class ElectronicTransmissionController1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Transmission Driveline Engaged", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Driveline_Engaged;

    @FieldModelDescription(description = "Transmission Torque Converter Lockup Engaged", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Torque_Converter_Lockup_Engaged;

    @FieldModelDescription(description = "Transmission Shift In Process", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Shift_In_Process;

    @FieldModelDescription(description = "Transmission Torque Converter Lockup Transition in Process", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Transmission_Torque_Converter_Lockup_Transition_in_Process;

    @FieldModelDescription(description = "Transmission 1 Output Shaft Speed", units = EngineeringUnits.revolutions_per_minute, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Transmission1_Output_Shaft_Speed;

    @FieldModelDescription(description = "Percent Clutch Slip", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Percent_Clutch_Slip;

    @FieldModelDescription(description = "Engine Momentary Overspeed Enable", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 0)
    public PgnActivationMode Engine_Momentary_Overspeed_Enable;

    @FieldModelDescription(description = "Progressive Shift Disable", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 2)
    public PgnActivationMode Progressive_Shift_Disable;

    @FieldModelDescription(description = "Momentary Engine Maximum Power Enable", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Momentary_Engine_Maximum_Power_Enable;

    @FieldModelDescription(description = "Transmission 1 Input Shaft Speed", units = EngineeringUnits.revolutions_per_minute, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 6, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Transmission1_Input_Shaft_Speed;

    @FieldModelDescription(description = "Source Address of Controlling Device for Transmission Control", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 8, width = 8, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 255)
    public Optional<Unsigned8> Source_Address_of_Controlling_Device_for_Transmission_Control;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "ElectronicTransmissionController1";
    }
}