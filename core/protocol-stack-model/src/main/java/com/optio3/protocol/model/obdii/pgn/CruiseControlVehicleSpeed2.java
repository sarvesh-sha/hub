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
import com.optio3.protocol.model.obdii.pgn.enums.PgnCruiseControlEnable;
import com.optio3.protocol.model.obdii.pgn.enums.PgnCruiseControlRequest;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:CruiseControlVehicleSpeed2")
@PgnMessageType(pgn = 2560, littleEndian = true, ignoreWhenReceived = false)
public class CruiseControlVehicleSpeed2 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Cruise Control Disable Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0)
    public PgnCruiseControlEnable Cruise_Control_Disable_Command;

    @FieldModelDescription(description = "Cruise Control Resume Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 2)
    public PgnCruiseControlRequest Cruise_Control_Resume_Command;

    @FieldModelDescription(description = "Cruise Control Pause Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 4)
    public PgnCruiseControlEnable Cruise_Control_Pause_Command;

    @FieldModelDescription(description = "Cruise Control Set Command", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 6)
    public PgnCruiseControlEnable Cruise_Control_Set_Command;

    @FieldModelDescription(description = "Idle Speed Request", units = EngineeringUnits.revolutions_per_minute, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 2, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Idle_Speed_Request;

    @FieldModelDescription(description = "Idle Control Enable State", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Idle_Control_Enable_State;

    @FieldModelDescription(description = "Idle Control Request Activation", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 4, width = 2, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Idle_Control_Request_Activation;

    @FieldModelDescription(description = "Remote Road Speed Limit Request", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 5, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 248)
    public float Remote_Road_Speed_Limit_Request;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "CruiseControlVehicleSpeed2";
    }
}