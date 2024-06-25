/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed.choice;

import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.serialization.SerializationTag;

public final class BACnetScale extends Choice
{
    @SerializationTag(number = 0)
    public float float_scale;

    @SerializationTag(number = 1)
    public int integer_scale;
}
