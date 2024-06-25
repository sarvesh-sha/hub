/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.List;
import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BACnetCOVMultipleSubscription extends Sequence
{
    public static class PropSpec extends Sequence
    {
        @SerializationTag(number = 0)
        public BACnetPropertyReference monitored_property;

        @SerializationTag(number = 1)
        public Optional<Float> cov_increment;

        @SerializationTag(number = 2)
        public boolean timestamped;
    }

    public static class Spec extends Sequence
    {
        @SerializationTag(number = 0)
        public BACnetObjectIdentifier monitored_object_identifier;

        @SerializationTag(number = 1)
        public List<PropSpec> list_of_cov_references;
    }

    @SerializationTag(number = 0)
    public BACnetRecipientProcess recipient;

    @SerializationTag(number = 1)
    public boolean issue_confirmed_notifications;

    @SerializationTag(number = 2)
    public Unsigned32 time_remaining;

    @SerializationTag(number = 3)
    public Unsigned32 max_notification_delay;

    @SerializationTag(number = 4)
    public List<Spec> list_of_cov_subscription_specifications;
}
