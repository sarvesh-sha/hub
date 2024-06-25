/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed.choice;

import com.optio3.protocol.model.bacnet.AnyValue;
import com.optio3.protocol.model.bacnet.constructed.BACnetDateTime;
import com.optio3.serialization.SerializationTag;

public final class BACnetPriorityValue extends AnyValue
{
    @SerializationTag(number = 0)
    public Object constructed_value; // The actual structure for this is undefined, we'll have to skip all the tags...

    @SerializationTag(number = 1)
    public BACnetDateTime datetime;
}
