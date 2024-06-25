/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.unconfirmed;

import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.bacnet.model.pdu.request.UnconfirmedServiceRequest;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.enums.BACnetMessagePriority;
import com.optio3.serialization.SerializationTag;

public class UnconfirmedTextMessage extends UnconfirmedServiceRequest
{
    public static class MessageClass extends Choice
    {
        @SerializationTag(number = 0)
        public Unsigned32 numeric;

        @SerializationTag(number = 1)
        public String character;
    }

    @SerializationTag(number = 0)
    public BACnetObjectIdentifier text_message_source_device;

    @SerializationTag(number = 1)
    public Optional<MessageClass> message_class;

    @SerializationTag(number = 2)
    public BACnetMessagePriority message_priority;

    @SerializationTag(number = 3)
    public String message;
}
