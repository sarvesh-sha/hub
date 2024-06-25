/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetTimeStamp;
import com.optio3.protocol.model.bacnet.enums.BACnetEventState;
import com.optio3.serialization.SerializationTag;

public final class AcknowledgeAlarm extends ConfirmedServiceRequest
{
    @SerializationTag(number = 0)
    public Unsigned32 acknowledging_process_identifier;

    @SerializationTag(number = 1)
    public BACnetObjectIdentifier event_object_identifier;

    @SerializationTag(number = 2)
    public BACnetEventState event_state_acknowledged;

    @SerializationTag(number = 3)
    public BACnetTimeStamp timestamp;

    @SerializationTag(number = 4)
    public String acknowledgment_source;

    @SerializationTag(number = 5)
    public BACnetTimeStamp time_of_acknowledgment;
}
