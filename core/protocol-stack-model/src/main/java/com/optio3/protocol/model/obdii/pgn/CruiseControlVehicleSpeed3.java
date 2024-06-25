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
import com.optio3.protocol.model.obdii.pgn.enums.PgnCruiseControlStatus;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:CruiseControlVehicleSpeed3")
@PgnMessageType(pgn = 64732, littleEndian = true, ignoreWhenReceived = false)
public class CruiseControlVehicleSpeed3 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Adaptive Cruise Control Readiness Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Adaptive_Cruise_Control_Readiness_Status;

    @FieldModelDescription(description = "Cruise Control System Command State", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 3, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnCruiseControlStatus Cruise_Control_System_Command_State;

    @FieldModelDescription(description = "Predictive Cruise Control Set Speed Offset Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Predictive_Cruise_Control_Set_Speed_Offset_Status;

    @FieldModelDescription(description = "Source Address of Controlling Device for Disabling Cruise Control", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 8, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 255)
    public Optional<Unsigned8> Source_Address_of_Controlling_Device_for_Disabling_Cruise_Control;

    @FieldModelDescription(description = "Source Address of Controlling Device for Pausing Cruise Control", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 8, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 255)
    public Optional<Unsigned8> Source_Address_of_Controlling_Device_for_Pausing_Cruise_Control;

    @FieldModelDescription(description = "AEBS readiness state", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode AEBS_readiness_state;

    @FieldModelDescription(description = "Cruise Control Driver Cancellation Status", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Cruise_Control_Driver_Cancellation_Status;

    @FieldModelDescription(description = "Cruise Control Set Speed (High Resolution)", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 63896)
    public float Cruise_Control_Set_Speed_High_Resolution;

    @FieldModelDescription(description = "Cruise Control Speed", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 63896)
    public float Cruise_Control_Speed;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "CruiseControlVehicleSpeed3";
    }
}