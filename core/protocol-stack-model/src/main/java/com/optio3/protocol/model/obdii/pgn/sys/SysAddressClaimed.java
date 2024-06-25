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

@JsonTypeName("Ipn:Obdii:Pgn:SysAddressClaimed")
@PgnMessageType(pgn = 60928, littleEndian = true)
public class SysAddressClaimed extends BaseSysPgnObjectModel
{
    @SerializationTag(number = 1, width = 21, bitOffset = 0, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int identity_number;

    @SerializationTag(number = 1, width = 11, bitOffset = 21, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int manufacturer_code;

    @SerializationTag(number = 5, width = 3, bitOffset = 0)
    public byte ECU_instance;

    @SerializationTag(number = 5, width = 5, bitOffset = 3, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int function_instance;

    @SerializationTag(number = 6, width = 8, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int function = 255; // = Not Available

    @SerializationTag(number = 7, width = 7, bitOffset = 1, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int vehicle_system = 127; // = Not Available

    @SerializationTag(number = 8, width = 4, bitOffset = 0, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int vehicle_system_instance;

    @SerializationTag(number = 8, width = 3, bitOffset = 4, scaling = { @SerializationScaling(assumeUnsigned = true) })
    public int industry_group;

    @SerializationTag(number = 8, width = 1, bitOffset = 7)
    public boolean arbitrary_address_capable;

    //--//

    @Override
    public String extractBaseId()
    {
        return "Pgn_" + "SysAddressClaimed";
    }
}
