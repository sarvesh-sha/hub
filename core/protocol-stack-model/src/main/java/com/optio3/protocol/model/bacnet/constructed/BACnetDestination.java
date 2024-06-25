/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.BACnetTime;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetRecipient;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetDaysOfWeek;
import com.optio3.protocol.model.bacnet.enums.bitstring.BACnetEventTransitionBits;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class BACnetDestination extends Sequence
{
    @SerializationTag(number = 0)
    @BACnetSerializationTag(untagged = true)
    public BACnetDaysOfWeek valid_days;

    @SerializationTag(number = 1)
    @BACnetSerializationTag(untagged = true)
    public BACnetTime from_time;

    @SerializationTag(number = 2)
    @BACnetSerializationTag(untagged = true)
    public BACnetTime to_time;

    @SerializationTag(number = 3)
    @BACnetSerializationTag(untagged = true)
    public BACnetRecipient recipient;

    @SerializationTag(number = 4)
    @BACnetSerializationTag(untagged = true)
    public Unsigned32 process_identifier;

    @SerializationTag(number = 5)
    @BACnetSerializationTag(untagged = true)
    public boolean issue_confirmed_notifications;

    @SerializationTag(number = 6)
    @BACnetSerializationTag(untagged = true)
    public BACnetEventTransitionBits transitions;
}
