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
import com.optio3.protocol.model.obdii.pgn.enums.PgnMainLightSwitch;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnTurnSignal;
import com.optio3.protocol.model.obdii.pgn.enums.PgnWorkLightSwitch;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:OperatorsExternalLightControlsMessage")
@PgnMessageType(pgn = 64972, littleEndian = true, ignoreWhenReceived = false)
public class OperatorsExternalLightControlsMessage extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Work Light Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 4, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnWorkLightSwitch Work_Light_Switch;

    @FieldModelDescription(description = "Main Light Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 4, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnMainLightSwitch Main_Light_Switch;

    @FieldModelDescription(description = "Turn Signal Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 4, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public PgnTurnSignal Turn_Signal_Switch;

    @FieldModelDescription(description = "Hazard Light Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 4, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Hazard_Light_Switch;

    @FieldModelDescription(description = "High-Low Beam Switch", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 6, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode High_Low_Beam_Switch;

    @FieldModelDescription(description = "Operators Desired Back-light", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(scalingFactor = 0.4, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Operators_Desired_Back_light;

    @FieldModelDescription(description = "Operators Desired - Delayed Lamp Off Time", units = EngineeringUnits.seconds, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 4, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Operators_Desired_Delayed_Lamp_Off_Time;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "OperatorsExternalLightControlsMessage";
    }
}