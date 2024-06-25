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
import com.optio3.protocol.model.obdii.pgn.enums.PgnDirectionSelector;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStarterConsent;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStarterRequest;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStarterStatus;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:EngineStartControl")
@PgnMessageType(pgn = 61677, littleEndian = true, ignoreWhenReceived = false)
public class EngineStartControl extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Engine Start Request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0)
    public PgnStarterRequest Engine_Start_Request;

    @FieldModelDescription(description = "Engine Start Consent", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 3, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnStarterConsent Engine_Start_Consent;

    @FieldModelDescription(description = "Transmission Shift Selector Requested Vehicle Direction", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 3, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnDirectionSelector Transmission_Shift_Selector_Requested_Vehicle_Direction;

    @FieldModelDescription(description = "Engine Start Abort Request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 0)
    public PgnActivationMode Engine_Start_Abort_Request;

    @FieldModelDescription(description = "Engine Starter 1 Feedback", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 3, bitOffset = 2, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnStarterStatus Engine_Starter1_Feedback;

    @FieldModelDescription(description = "Engine Starter 2 Feedback", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 3, bitOffset = 5, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 7)
    public PgnStarterStatus Engine_Starter2_Feedback;

    @FieldModelDescription(description = "Engine Start Control Message Counter", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 7, width = 4, bitOffset = 4, scaling = { @SerializationScaling(scalingFactor = 16.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 15)
    public Optional<Unsigned8> Engine_Start_Control_Message_Counter;

    @FieldModelDescription(description = "Engine Start Control Checksum", units = EngineeringUnits.counts, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 8, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Engine_Start_Control_Checksum;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "EngineStartControl";
    }
}