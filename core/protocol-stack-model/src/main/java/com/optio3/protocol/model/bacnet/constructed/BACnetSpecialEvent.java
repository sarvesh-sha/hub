/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.List;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetCalendarEntry;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class BACnetSpecialEvent extends Sequence
{
    @SerializationTag(number = 0)
    @BACnetSerializationTag(choiceSet = "period")
    public BACnetCalendarEntry calendar_entry;

    @SerializationTag(number = 1)
    @BACnetSerializationTag(choiceSet = "period")
    public BACnetObjectIdentifier calendar_reference;

    @SerializationTag(number = 2)
    public List<BACnetTimeValue> list_of_time_values;

    @SerializationTag(number = 3)
    public Unsigned8 event_priority;
}
