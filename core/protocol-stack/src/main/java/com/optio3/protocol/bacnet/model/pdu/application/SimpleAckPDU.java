/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.application;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.protocol.bacnet.model.enums.ConfirmedServiceChoice;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;

public final class SimpleAckPDU extends ApplicationPDU
{
    @SerializationTag(number = 1)
    public Unsigned8 invokeId;

    @SerializationTag(number = 2, width = 8)
    public ConfirmedServiceChoice serviceChoice;

    //--//

    public SimpleAckPDU(InputBuffer buffer)
    {
        super(buffer);
    }

    @Override
    public void dispatch(ServiceContext sc)
    {
        sc.processResponse(new ConfirmedServiceResponse.NoDataReply(), invokeId.unbox(), serviceChoice);
    }
}
