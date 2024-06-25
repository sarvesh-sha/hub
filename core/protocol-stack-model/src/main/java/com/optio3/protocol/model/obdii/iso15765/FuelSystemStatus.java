/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.iso15765;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.obdii.Iso15765MessageType;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:ISO15765:FuelSystemStatus")
@Iso15765MessageType(service = 1, pdu = 3)
public class FuelSystemStatus extends BaseIso15765ObjectModel
{
    @SerializationTag(number = 0, width = 8)
    public Unsigned8 system1;

    @SerializationTag(number = 1, width = 8)
    public Unsigned8 system2;

    //--//

    @Override
    public String extractBaseId()
    {
        return "ISO15765_FuelSystemStatus";
    }
}
