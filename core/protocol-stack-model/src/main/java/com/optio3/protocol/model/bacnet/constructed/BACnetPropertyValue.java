/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class BACnetPropertyValue extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetPropertyIdentifierOrUnknown property_identifier;

    @SerializationTag(number = 1)
    @BACnetSerializationTag(propertyIndex = true)
    public Optional<Unsigned32> property_array_index;

    @SerializationTag(number = 2)
    public Object property_value; // Depends on object_identifier & property_identifier

    @SerializationTag(number = 3)
    public Optional<Unsigned8> priority;
}
