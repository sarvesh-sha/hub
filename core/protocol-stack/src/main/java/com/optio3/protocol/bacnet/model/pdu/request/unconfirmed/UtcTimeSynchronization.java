/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.unconfirmed;

import com.optio3.protocol.bacnet.model.pdu.request.UnconfirmedServiceRequest;
import com.optio3.protocol.model.bacnet.constructed.BACnetDateTime;
import com.optio3.serialization.SerializationTag;

public class UtcTimeSynchronization extends UnconfirmedServiceRequest
{
    @SerializationTag(number = 0)
    public BACnetDateTime time;
}
