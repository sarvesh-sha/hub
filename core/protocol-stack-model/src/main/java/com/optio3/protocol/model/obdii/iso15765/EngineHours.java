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

@JsonTypeName("Ipn:Obdii:ISO15765:EngineHours")
@Iso15765MessageType(service = 1, pdu = 127, hasMultipleFrames = true)
public class EngineHours extends BaseIso15765ObjectModel
{
    @SerializationTag(number = 0, width = 8)
    public int flags;

    // Limit to an update every 15 minutes of runtime.
    @FieldModelDescription(description = "Engine Runtime1", units = EngineeringUnits.seconds, pointClass = WellKnownPointClass.ObdiiEngineRuntimeTotal, debounceSeconds = 5, minimumDelta = 900)
    @SerializationTag(number = 1, width = 32, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int time1;

    // Limit to an update every 15 minutes of runtime.
    @FieldModelDescription(description = "Engine Runtime2", units = EngineeringUnits.seconds, pointClass = WellKnownPointClass.ObdiiEngineRuntime, debounceSeconds = 5, minimumDelta = 900)
    @SerializationTag(number = 2, width = 32, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int time2;

    //--//

    @Override
    public String extractBaseId()
    {
        return "ISO15765_EngineHours";
    }
}
