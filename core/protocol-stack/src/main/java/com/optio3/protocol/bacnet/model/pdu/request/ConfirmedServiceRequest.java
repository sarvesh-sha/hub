/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.bacnet.model.enums.ConfirmedServiceChoice;
import com.optio3.protocol.bacnet.model.pdu.application.ConfirmedRequestPDU;

public abstract class ConfirmedServiceRequest extends ServiceRequest
{
    public ConfirmedServiceChoice getChoice()
    {
        return ConfirmedServiceChoice.lookupRequest(getClass());
    }

    public ConfirmedRequestPDU preparePCI()
    {
        ConfirmedRequestPDU res = new ConfirmedRequestPDU(null);
        res.serviceChoice = getChoice();
        res.invokeId      = Unsigned8.box(0);
        return res;
    }
}
