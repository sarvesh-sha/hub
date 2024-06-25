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

@JsonTypeName("Ipn:Obdii:ISO15765:DtcCheck")
@Iso15765MessageType(service = 1, pdu = 1)
public class DtcCheck extends BaseIso15765ObjectModel
{
    @SerializationTag(number = 0, width = 1, bitOffset = 7)
    public boolean mil_active;

    @SerializationTag(number = 0, width = 7, bitOffset = 0, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int dct_count;

    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int b;

    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int c;

    @SerializationTag(number = 3, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int d;

    //--//

    @Override
    public String extractBaseId()
    {
        return "ISO15765_DtcCheck";
    }
}
