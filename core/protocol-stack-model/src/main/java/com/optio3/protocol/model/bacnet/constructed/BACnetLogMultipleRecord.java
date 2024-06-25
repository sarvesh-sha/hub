/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetLogData;
import com.optio3.serialization.SerializationTag;

public final class BACnetLogMultipleRecord extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetDateTime timestamp;

    @SerializationTag(number = 1)
    public BACnetLogData log_data;
}
