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

@JsonTypeName("Ipn:Obdii:ISO15765:TimeRunWithMalfunction")
@Iso15765MessageType(service = 1, pdu = 77)
public class TimeRunWithMalfunction extends BaseIso15765ObjectModel
{
    @FieldModelDescription(description = "Time run with MIL on", units = EngineeringUnits.minutes, pointClass = WellKnownPointClass.ObdiiTimeRunWithMalfunction, debounceSeconds = 5, minimumDelta = 10)
    @SerializationTag(number = 0, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int value;

    //--//

    @Override
    public String extractBaseId()
    {
        return "ISO15765_TimeRunWithMalfunction";
    }
}
