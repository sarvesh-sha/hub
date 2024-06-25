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

@JsonTypeName("Ipn:Obdii:ISO15765:TimingAdvance")
@Iso15765MessageType(service = 1, pdu = 14)
public class TimingAdvance extends BaseIso15765ObjectModel
{
    @SerializationTag(number = 0, width = 8, scaling = { @SerializationScaling(scalingFactor = 1.0 / 2, postScalingOffset = -64, assumeUnsigned = true) })
    public float value;

    //--//

    @Override
    public String extractBaseId()
    {
        return "ISO15765_TimingAdvance";
    }
}
