/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class BACnetDeviceObjectPropertyReference extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetObjectIdentifier object_identifier;

    @SerializationTag(number = 1)
    public BACnetPropertyIdentifierOrUnknown property_identifier;

    @SerializationTag(number = 2)
    @BACnetSerializationTag(propertyIndex = true)
    public Optional<Unsigned32> property_array_index;

    @SerializationTag(number = 3)
    public Optional<BACnetObjectIdentifier> device_identifier;
}
