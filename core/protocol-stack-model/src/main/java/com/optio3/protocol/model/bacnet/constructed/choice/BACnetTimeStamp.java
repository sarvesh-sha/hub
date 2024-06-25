/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed.choice;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.BACnetTime;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.constructed.BACnetDateTime;
import com.optio3.serialization.SerializationTag;

public final class BACnetTimeStamp extends Choice
{
    @SerializationTag(number = 0)
    public BACnetTime time;

    @SerializationTag(number = 1)
    public Unsigned32 sequence_number;

    @SerializationTag(number = 2)
    public BACnetDateTime datetime;
}
