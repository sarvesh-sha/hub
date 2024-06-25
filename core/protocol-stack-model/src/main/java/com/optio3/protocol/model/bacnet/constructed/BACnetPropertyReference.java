/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class BACnetPropertyReference extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetPropertyIdentifierOrUnknown property_identifier;

    @SerializationTag(number = 1)
    @BACnetSerializationTag(propertyIndex = true)
    public Optional<Unsigned32> property_array_index; // used only with array datatype if omitted with an array the entire array is referenced

    //--//

    public static BACnetPropertyReference newInstance(BACnetPropertyIdentifierOrUnknown prop)
    {
        BACnetPropertyReference res = new BACnetPropertyReference();
        res.property_identifier = prop;
        return res;
    }

    public static BACnetPropertyReference newInstance(BACnetPropertyIdentifierOrUnknown prop,
                                                      int index)
    {
        BACnetPropertyReference res = newInstance(prop);
        res.property_array_index = Optional.of(Unsigned32.box(index));
        return res;
    }

    public static BACnetPropertyReference newInstance(BACnetPropertyIdentifier prop)
    {
        return newInstance(prop.forRequest());
    }

    public static BACnetPropertyReference newInstance(BACnetPropertyIdentifier prop,
                                                      int index)
    {
        return newInstance(prop.forRequest(), index);
    }
}
