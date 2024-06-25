/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.Optional;

import com.optio3.protocol.model.bacnet.AnyValue;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BACnetNameValue extends Sequence
{
    @SerializationTag(number = 0)
    public String name;

    @SerializationTag(number = 1)
    public Optional<AnyValue> value; // value is limited to primitive datatypes and BACnetDateTime
}
