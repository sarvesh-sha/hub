/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.sys;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.obdii.PgnMessageType;
import com.optio3.protocol.model.obdii.pgn.enums.PgnIsoTransportLayerType;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;

@JsonTypeName("Ipn:Obdii:Pgn:SysIsoTransportLayer")
@PgnMessageType(pgn = 0xDA00, littleEndian = true)
public class SysIsoTransportLayer extends BaseSysPgnObjectModel
{
    @SerializationTag(number = 0, width = 4, bitOffset = 4, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public PgnIsoTransportLayerType type;

    @SerializationTag(number = 0, width = 4, bitOffset = 0, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int extra;

    @SerializationTag(number = 1)
    public byte[] payload;

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "SysIsoTransportLayer";
    }
}
