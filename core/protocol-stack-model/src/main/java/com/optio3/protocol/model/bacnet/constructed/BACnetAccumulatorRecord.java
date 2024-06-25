/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BACnetAccumulatorRecord extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetDateTime timestamp;

    @SerializationTag(number = 1)
    public Unsigned32 present_value;

    @SerializationTag(number = 2)
    public Unsigned32 accumulated_value;

    @SerializationTag(number = 3)
    public Unsigned32 accumulator_status;
}
