/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.unconfirmed;

import java.util.List;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.bacnet.model.pdu.request.UnconfirmedServiceRequest;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.constructed.BACnetPropertyValue;
import com.optio3.serialization.SerializationTag;

public final class UnconfirmedCOVNotification extends UnconfirmedServiceRequest
{
    @SerializationTag(number = 0)
    public Unsigned32 subscriber_process_identifier;

    @SerializationTag(number = 1)
    public BACnetObjectIdentifier initiating_device_identifier;

    @SerializationTag(number = 2)
    public BACnetObjectIdentifier monitored_object_identifier;

    @SerializationTag(number = 3)
    public Unsigned32 time_remaining;

    @SerializationTag(number = 4)
    public List<BACnetPropertyValue> list_of_values;
}
