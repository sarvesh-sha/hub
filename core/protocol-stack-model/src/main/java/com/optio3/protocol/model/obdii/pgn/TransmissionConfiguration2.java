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

@JsonTypeName("Ipn:Obdii:Pgn:TransmissionConfiguration2")
@PgnMessageType(pgn = 65099, littleEndian = true, ignoreWhenReceived = false)
public class TransmissionConfiguration2 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "Transmission Torque Limit", units = EngineeringUnits.newton_meters, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Transmission_Torque_Limit;

    @FieldModelDescription(description = "Transmission Reference Torque", units = EngineeringUnits.newton_meters, debounceSeconds = 15, noValueMarker = -100.000000)
    @SerializationTag(number = 3, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 65535)
    public float Transmission_Reference_Torque;

    @FieldModelDescription(description = "Transmission Communications Failure Idle Torque Limit", units = EngineeringUnits.percent, debounceSeconds = 15, noValueMarker = -225.000000)
    @SerializationTag(number = 5, width = 8, scaling = { @SerializationScaling(postScalingOffset = -125.0, assumeUnsigned = true) }, preProcessor = BasePgnObjectModel.DetectOutOfRange.class, preProcessorUpperRange = 255)
    public float Transmission_Communications_Failure_Idle_Torque_Limit;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "TransmissionConfiguration2";
    }
}