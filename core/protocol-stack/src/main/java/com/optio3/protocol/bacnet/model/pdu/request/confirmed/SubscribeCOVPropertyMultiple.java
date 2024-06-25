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
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.constructed.BACnetPropertyReference;
import com.optio3.serialization.SerializationTag;

public final class SubscribeCOVPropertyMultiple extends ConfirmedServiceRequest
{
    public static final class Specifications extends Sequence
    {
        @SerializationTag(number = 0)
        public BACnetPropertyReference monitored_property;

        @SerializationTag(number = 1)
        public Optional<Float> cov_increment;

        @SerializationTag(number = 2)
        public boolean timestamped;
    }

    @SerializationTag(number = 0)
    public Unsigned32 subscriber_process_identifier;

    @SerializationTag(number = 1)
    public Optional<Boolean> issue_confirmed_notifications;

    @SerializationTag(number = 2)
    public Optional<Unsigned32> lifetime;

    @SerializationTag(number = 3)
    public Optional<Unsigned32> max_notification_delay;

    @SerializationTag(number = 4)
    public List<Specifications> list_of_cov_subscription_specifications = Lists.newArrayList();
}
