/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.sys;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("Ipn:Obdii:Pgn:Proprietary")
public class Proprietary extends BaseSysPgnObjectModel
{
    public int    pgn;
    public String payload;

    //--//

    @Override
    public String extractBaseId()
    {
        return String.format("Pgn_Proprietary_%x", pgn);
    }
}
