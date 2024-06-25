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
import com.optio3.protocol.model.obdii.pgn.enums.PgnAntiTheftCommand;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:AntiTheftRequest")
@PgnMessageType(pgn = 56576, littleEndian = true, ignoreWhenReceived = false)
public class AntiTheftRequest extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Anti-theft Encryption Indicator States", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 1, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Anti_theft_Encryption_Indicator_States;

    @FieldModelDescription(description = "Anti-theft Desired Exit Mode States", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 3, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Anti_theft_Desired_Exit_Mode_States;

    @FieldModelDescription(description = "Anti-theft Command States", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 3, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnAntiTheftCommand Anti_theft_Command_States;

    @FieldModelDescription(description = "Anti-theft Password Representation", units = EngineeringUnits.no_units, debounceSeconds = 15)
    @SerializationTag(number = 2)
    public byte[] Anti_theft_Password_Representation;

    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "AntiTheftRequest";
    }
}