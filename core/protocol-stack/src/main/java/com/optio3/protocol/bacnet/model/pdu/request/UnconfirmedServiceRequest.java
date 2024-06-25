/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request;

import com.optio3.protocol.bacnet.model.enums.UnconfirmedServiceChoice;
import com.optio3.protocol.bacnet.model.pdu.application.UnconfirmedRequestPDU;

public abstract class UnconfirmedServiceRequest extends ServiceRequest
{
    public UnconfirmedServiceChoice getChoice()
    {
        return UnconfirmedServiceChoice.lookup(getClass());
    }

    public UnconfirmedRequestPDU preparePCI()
    {
        UnconfirmedRequestPDU res = new UnconfirmedRequestPDU(null);
        res.serviceChoice = getChoice();
        return res;
    }
}
