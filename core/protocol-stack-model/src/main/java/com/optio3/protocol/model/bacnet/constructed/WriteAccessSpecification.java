/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.serialization.SerializationTag;

public final class WriteAccessSpecification extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetObjectIdentifier object_identifier;

    @SerializationTag(number = 1)
    public List<BACnetPropertyValue> list_of_properties = Lists.newArrayList();

    //--//

    public static WriteAccessSpecification newRequest(BACnetObjectIdentifier objId,
                                                      BACnetPropertyIdentifierOrUnknown prop,
                                                      Object value)
    {
        WriteAccessSpecification res = new WriteAccessSpecification();
        res.object_identifier = objId;

        res.add(prop, value);

        return res;
    }

    public static WriteAccessSpecification newRequest(BACnetObjectIdentifier objId,
                                                      BACnetPropertyIdentifierOrUnknown prop,
                                                      int index,
                                                      Object value)
    {
        WriteAccessSpecification res = new WriteAccessSpecification();
        res.object_identifier = objId;

        res.add(prop, index, value);

        return res;
    }

    public static WriteAccessSpecification newRequest(BACnetObjectIdentifier objId,
                                                      BACnetPropertyIdentifier prop,
                                                      Object value)
    {
        return newRequest(objId, prop.forRequest(), value);
    }

    public static WriteAccessSpecification newRequest(BACnetObjectIdentifier objId,
                                                      BACnetPropertyIdentifier prop,
                                                      int index,
                                                      Object value)
    {
        return newRequest(objId, prop.forRequest(), index, value);
    }

    //--//

    public void add(BACnetPropertyIdentifierOrUnknown prop,
                    Object value)
    {
        BACnetPropertyValue val = new BACnetPropertyValue();
        val.property_identifier = prop;
        val.property_value = value;

        list_of_properties.add(val);
    }

    public void add(BACnetPropertyIdentifierOrUnknown prop,
                    int index,
                    Object value)
    {
        BACnetPropertyValue val = new BACnetPropertyValue();
        val.property_identifier = prop;
        val.property_array_index = Optional.of(Unsigned32.box(index));
        val.property_value = value;

        list_of_properties.add(val);
    }
}
