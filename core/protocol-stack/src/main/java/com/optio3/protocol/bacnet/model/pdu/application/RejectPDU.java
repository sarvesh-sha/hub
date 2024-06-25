/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.application;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.protocol.model.bacnet.enums.BACnetRejectReason;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;

public final class RejectPDU extends ApplicationPDU
{
    @SerializationTag(number = 1)
    public Unsigned8 invokeId;

    @SerializationTag(number = 2, width = 8)
    public BACnetRejectReason rejectReason;

    //--//

    public RejectPDU(InputBuffer buffer)
    {
        super(buffer);
    }

    @Override
    public void dispatch(ServiceContext sc)
    {
        sc.processReject(this);
    }
}
