/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.iso15765;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.FieldModelDescription;
import com.optio3.protocol.model.WellKnownPointClass;
import com.optio3.protocol.model.obdii.Iso15765MessageType;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:ISO15765:Odometer")
@Iso15765MessageType(service = 1, pdu = 166)
public class Odometer extends BaseIso15765ObjectModel
{
    @FieldModelDescription(description = "Odometer", units = EngineeringUnits.meters, pointClass = WellKnownPointClass.ObdiiOdometer, debounceSeconds = 5, minimumDelta = 5)
    @SerializationTag(number = 0, width = 32, scaling = { @SerializationScaling(assumeUnsigned = true, scalingFactor = 100) })
    public int value;

    //--//

    @Override
    public String extractBaseId()
    {
        return "ISO15765_Odometer";
    }
}
