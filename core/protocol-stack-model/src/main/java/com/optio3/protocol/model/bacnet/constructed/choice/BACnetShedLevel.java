/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed.choice;

import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.serialization.SerializationTag;

public final class BACnetShedLevel extends Choice
{
    @SerializationTag(number = 0)
    public Unsigned32 percent;

    @SerializationTag(number = 1)
    public Unsigned32 level;

    @SerializationTag(number = 2)
    public Optional<Float> amount;
}
