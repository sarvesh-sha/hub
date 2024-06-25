/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.palfinger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.can.CanMessageType;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Can:Palfinger_ManufacturerIdent2")
@CanMessageType(sourceAddress = 0x681, littleEndian = false)
public class Palfinger_ManufacturerIdent2 extends BasePalfingerModel
{
    @SerializationTag(number = 0, width = 8)
    public char[] text;

    //--//

    @Override
    public String extractBaseId()
    {
        return "Palfinger_ManufacturerIdent2";
    }
}
