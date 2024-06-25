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
import com.optio3.protocol.model.obdii.pgn.enums.PgnActivationMode;
import com.optio3.protocol.model.obdii.pgn.enums.PgnStatusMode;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:VehicleDynamicStabilityControl1")
@PgnMessageType(pgn = 65103, littleEndian = true, ignoreWhenReceived = false)
public class VehicleDynamicStabilityControl1 extends BasePgnObjectModel
{
    // @formatter:off
    @FieldModelDescription(description = "VDC Information Signal", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 0)
    public PgnActivationMode VDC_Information_Signal;

    @FieldModelDescription(description = "VDC Fully Operational", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 2)
    public PgnActivationMode VDC_Fully_Operational;

    @FieldModelDescription(description = "VDC brake light request", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 1, width = 2, bitOffset = 4)
    public PgnActivationMode VDC_brake_light_request;

    @FieldModelDescription(description = "ROP Engine Control active", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 0)
    public PgnActivationMode ROP_Engine_Control_active;

    @FieldModelDescription(description = "ROP Brake Control active", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 2)
    public PgnActivationMode ROP_Brake_Control_active;

    @FieldModelDescription(description = "YC Engine Control active", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 4)
    public PgnActivationMode YC_Engine_Control_active;

    @FieldModelDescription(description = "YC Brake Control active", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 2, width = 2, bitOffset = 6)
    public PgnActivationMode YC_Brake_Control_active;

    @FieldModelDescription(description = "Trailer-VDC Active", units = EngineeringUnits.enumerated, debounceSeconds = 5)
    @SerializationTag(number = 3, width = 2, bitOffset = 0, preProcessor = BasePgnObjectModel.DetectMissing.class, preProcessorUpperRange = 3)
    public PgnStatusMode Trailer_VDC_Active;
    // @formatter:on

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "VehicleDynamicStabilityControl1";
    }
}