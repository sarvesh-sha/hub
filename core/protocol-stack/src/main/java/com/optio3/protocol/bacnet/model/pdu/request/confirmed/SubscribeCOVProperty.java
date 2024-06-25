/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.constructed.BACnetPropertyReference;
import com.optio3.serialization.SerializationTag;

public final class SubscribeCOVProperty extends ConfirmedServiceRequest
{
    @SerializationTag(number = 0)
    public Unsigned32 subscriber_process_identifier;

    @SerializationTag(number = 1)
    public BACnetObjectIdentifier monitored_object_identifier;

    @SerializationTag(number = 2)
    public Optional<Boolean> issue_confirmed_notifications;

    @SerializationTag(number = 3)
    public Optional<Unsigned32> lifetime;

    @SerializationTag(number = 4)
    public BACnetPropertyReference monitored_property_identifier;

    @SerializationTag(number = 5)
    public Optional<Float> cov_increment;
}
