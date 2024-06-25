/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("Ipn:Obdii:UnknownPdu")
public class UnknownPdu extends ObdiiObjectModel
{
    public int     priority;
    public boolean extendedDataPage;
    public boolean dataPage;
    public int     pduFormat;
    public int     destinationAddress;
    public String  payload;

    //--//

    public boolean shouldIncludeObject()
    {
        // Ignore unknown messages.
        return false;
    }

    public boolean shouldIncludeProperty(String prop)
    {
        // Ignore all properties.
        return false;
    }

    //--//

    @Override
    public String extractBaseId()
    {
        return String.format("Pgn_UnknownPdu_%d_%d_%d_%d", extendedDataPage ? 1 : 0, dataPage ? 1 : 0, pduFormat, destinationAddress);
    }
}
