/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.protocol.model.bacnet.BACnetDate;
import com.optio3.protocol.model.bacnet.BACnetTime;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class BACnetDateTime extends Sequence
{
    @SerializationTag(number = 0)
    @BACnetSerializationTag(untagged = true)
    public BACnetDate date;

    @SerializationTag(number = 1)
    @BACnetSerializationTag(untagged = true)
    public BACnetTime time;
}
