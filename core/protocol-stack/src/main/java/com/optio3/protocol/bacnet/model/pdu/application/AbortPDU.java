/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.application;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.protocol.model.bacnet.enums.BACnetAbortReason;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;

//
//  | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
//  |---|---|---|---|---|---|---|---|
//  | PDU Type      | 0 | 0 | 0 |SRV|
//  |---|---|---|---|---|---|---|---|
//  | Original Invoke ID            |
//  |---|---|---|---|---|---|---|---|
//  | Abort Reason                  |
//  |---|---|---|---|---|---|---|---|
//
public final class AbortPDU extends ApplicationPDU
{
    @SerializationTag(number = 0, bitOffset = 0, width = 1)
    public boolean server;

    @SerializationTag(number = 1)
    public Unsigned8 invokeId;

    @SerializationTag(number = 2, width = 8)
    public BACnetAbortReason abortReason;

    //--//

    public AbortPDU(InputBuffer buffer)
    {
        super(buffer);
    }

    @Override
    public void dispatch(ServiceContext sc)
    {
        sc.processAbort(this);
    }
}
