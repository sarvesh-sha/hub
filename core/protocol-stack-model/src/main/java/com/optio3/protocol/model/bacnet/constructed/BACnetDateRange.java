/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.protocol.model.bacnet.BACnetDate;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class BACnetDateRange extends Sequence
{
    @SerializationTag(number = 0)
    @BACnetSerializationTag(untagged = true)
    public BACnetDate start_date;

    @SerializationTag(number = 1)
    @BACnetSerializationTag(untagged = true)
    public BACnetDate end_date;
}
