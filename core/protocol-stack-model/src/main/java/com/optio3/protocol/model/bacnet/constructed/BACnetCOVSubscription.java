/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BACnetCOVSubscription extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetRecipientProcess recipient;

    @SerializationTag(number = 1)
    public BACnetObjectPropertyReference monitored_property_reference;

    @SerializationTag(number = 2)
    public boolean issue_confirmed_notifications;

    @SerializationTag(number = 3)
    public Unsigned32 time_remaining;

    @SerializationTag(number = 4)
    public Optional<Float> cov_increment; // used only with monitored properties with a numeric datatype
}
