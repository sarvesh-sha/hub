/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetTime;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.constructed.BACnetDateTime;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class ConfirmedCOVNotificationMultiple extends ConfirmedServiceRequest
{
    public static final class Values extends Sequence
    {
        @SerializationTag(number = 0)
        public BACnetPropertyIdentifierOrUnknown property_identifier;

        @SerializationTag(number = 1)
        @BACnetSerializationTag(propertyIndex = true)
        public Optional<Unsigned32> property_array_index;

        @SerializationTag(number = 2)
        public Object property_value; // Depends on object_identifier & property_identifier

        @SerializationTag(number = 3)
        public Optional<BACnetTime> time_of_change;
    }

    public static final class Notifications extends Sequence
    {
        @SerializationTag(number = 0)
        public BACnetObjectIdentifier monitored_object_identifier;

        @SerializationTag(number = 1)
        public List<Values> list_of_values = Lists.newArrayList();
    }

    @SerializationTag(number = 0)
    public Unsigned32 subscriber_process_identifier;

    @SerializationTag(number = 1)
    public BACnetObjectIdentifier initiating_device_identifier;

    @SerializationTag(number = 2)
    public Unsigned32 time_remaining;

    @SerializationTag(number = 3)
    public Optional<BACnetDateTime> timestamp;

    @SerializationTag(number = 4)
    public List<Notifications> list_of_cov_notifications = Lists.newArrayList();
}
