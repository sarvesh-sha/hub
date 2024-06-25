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

public final class BACnetPropertyAccessResult extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetObjectIdentifier object_identifier;

    @SerializationTag(number = 1)
    public BACnetPropertyIdentifierOrUnknown property_identifier;

    @SerializationTag(number = 2)
    @BACnetSerializationTag(propertyIndex = true)
    public Optional<Unsigned32> property_array_index; // used only with array datatype if omitted with an array the entire array is referenced

    @SerializationTag(number = 3)
    public Optional<BACnetObjectIdentifier> device_identifier;

    @SerializationTag(number = 4)
    @BACnetSerializationTag(choiceSet = "access-result")
    public Object property_value; // Depends on object_identifier & property_identifier

    @SerializationTag(number = 5)
    @BACnetSerializationTag(choiceSet = "access-result")
    public BACnetError property_access_error;
}
