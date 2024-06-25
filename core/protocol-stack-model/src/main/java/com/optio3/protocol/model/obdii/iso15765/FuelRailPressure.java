/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.iso15765;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.obdii.Iso15765MessageType;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:ISO15765:FuelRailPressure")
@Iso15765MessageType(service = 1, pdu = 34)
public class FuelRailPressure extends BaseIso15765ObjectModel
{
    @SerializationTag(number = 0, width = 16, scaling = { @SerializationScaling(scalingFactor = 0.079, assumeUnsigned = true) })
    public float value;

    //--//

    @Override
    public String extractBaseId()
    {
        return "ISO15765_FuelRailPressure";
    }
}
