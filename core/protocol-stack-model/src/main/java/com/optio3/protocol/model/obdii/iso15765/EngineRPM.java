/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.iso15765;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.obdii.Iso15765MessageType;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:ISO15765:EngineRPM")
@Iso15765MessageType(service = 1, pdu = 12)
public class EngineRPM extends BaseIso15765ObjectModel
{
    @FieldModelDescription(description = "Engine RPM", units = EngineeringUnits.revolutions_per_minute, pointClass = WellKnownPointClass.ObdiiEngineRPM, debounceSeconds = 15, minimumDelta = 100)
    @SerializationTag(number = 0, width = 16, scaling = { @SerializationScaling(scalingFactor = 1.0 / 4, assumeUnsigned = true) })
    public float value;

    //--//

    @Override
    public String extractBaseId()
    {
        return "ISO15765_EngineRPM";
    }
}
