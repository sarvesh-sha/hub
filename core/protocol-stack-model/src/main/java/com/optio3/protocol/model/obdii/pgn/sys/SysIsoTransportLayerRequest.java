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

@JsonTypeName("Ipn:Obdii:Pgn:SysIsoTransportLayerRequest")
@PgnMessageType(pgn = 0xDB00, littleEndian = true)
public class SysIsoTransportLayerRequest extends BaseSysPgnObjectModel
{
    @SerializationTag(number = 0, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int length;

    @SerializationTag(number = 1, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public byte service;

    @SerializationTag(number = 2, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public byte pdu;

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "SysIsoTransportLayerRequest";
    }
}
