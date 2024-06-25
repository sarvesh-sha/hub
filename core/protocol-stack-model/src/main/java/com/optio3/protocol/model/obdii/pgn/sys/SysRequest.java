/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.sys;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.obdii.PgnMessageType;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:SysRequest")
@PgnMessageType(pgn = 59904, littleEndian = true)
public class SysRequest extends BaseSysPgnObjectModel
{
    @SerializationTag(number = 1, width = 24, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int pgn;

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "SysRequest";
    }
}
