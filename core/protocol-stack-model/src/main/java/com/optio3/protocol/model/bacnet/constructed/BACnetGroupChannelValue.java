/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.Optional;

import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BACnetGroupChannelValue extends Sequence
{
    @SerializationTag(number = 0)
    public Unsigned16 channel;

    @SerializationTag(number = 1)
    public Optional<Unsigned8> overriding_priority;

    @SerializationTag(number = 2)
    public BACnetChannelValue value;
}
