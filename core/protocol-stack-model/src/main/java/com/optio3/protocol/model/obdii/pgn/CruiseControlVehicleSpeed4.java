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
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:CruiseControlVehicleSpeed4")
@PgnMessageType(pgn = 64555, littleEndian = true, ignoreWhenReceived = false)
public class CruiseControlVehicleSpeed4 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Applied Vehicle Speed Limit (High Resolution)", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 63896)
    public float Applied_Vehicle_Speed_Limit_High_Resolution;

    @FieldModelDescription(description = "Cruise Control Adjusted Maximum Speed", units = EngineeringUnits.kilometers_per_hour, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.00390625, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 63896)
    public float Cruise_Control_Adjusted_Maximum_Speed;

    @FieldModelDescription(description = "Engine External Idle Request  Feedback", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 5, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Engine_External_Idle_Request_Feedback;

    @FieldModelDescription(description = "Source Address of Controlling Device for Setting Cruise Control", units = EngineeringUnits.no_units, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 6, width = 8, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 255)
    public Optional<Unsigned8> Source_Address_of_Controlling_Device_for_Setting_Cruise_Control;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "CruiseControlVehicleSpeed4";
    }
}