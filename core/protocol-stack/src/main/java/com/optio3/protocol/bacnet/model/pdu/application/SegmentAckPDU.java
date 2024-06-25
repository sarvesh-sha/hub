/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.application;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;

public final class SegmentAckPDU extends ApplicationPDU
{
    @SerializationTag(number = 0, bitOffset = 1, width = 1)
    public boolean negativeAck;

    @SerializationTag(number = 0, bitOffset = 0, width = 1)
    public boolean server;

    @SerializationTag(number = 1)
    public Unsigned8 invokeId;

    @SerializationTag(number = 2)
    public Unsigned8 sequenceNumber;

    @SerializationTag(number = 3)
    public Unsigned8 proposedWindowSize;

    //--//

    public SegmentAckPDU(InputBuffer buffer)
    {
        super(buffer);
    }

    @Override
    public void dispatch(ServiceContext sc)
    {
        sc.processSegmentAck(this);
    }
}
