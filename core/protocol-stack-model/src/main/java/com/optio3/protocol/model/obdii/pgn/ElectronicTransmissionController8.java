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
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:ElectronicTransmissionController8")
@PgnMessageType(pgn = 61452, littleEndian = true, ignoreWhenReceived = false)
public class ElectronicTransmissionController8 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Transmission Torque Converter Ratio", units = EngineeringUnits.counts, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.001, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Transmission_Torque_Converter_Ratio;

    @FieldModelDescription(description = "Transmission Clutch/Converter Input Speed", units = EngineeringUnits.revolutions_per_minute, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.125, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Transmission_Clutch_Converter_Input_Speed;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "ElectronicTransmissionController8";
    }
}