/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.serialization.SerializationTag;

public final class ReadAccessSpecification extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetObjectIdentifier object_identifier;

    @SerializationTag(number = 1)
    public List<BACnetPropertyReference> list_of_property_references = Lists.newArrayList();

    //--//

    public static ReadAccessSpecification newRequest(BACnetObjectIdentifier objId)
    {
        ReadAccessSpecification res = new ReadAccessSpecification();
        res.object_identifier = objId;

        return res;
    }

    public static ReadAccessSpecification newRequest(BACnetObjectIdentifier objId,
                                                     BACnetPropertyIdentifier prop)
    {
        ReadAccessSpecification res = newRequest(objId);
        res.add(prop);

        return res;
    }

    public static ReadAccessSpecification newRequest(BACnetObjectIdentifier objId,
                                                     BACnetPropertyIdentifier... props)
    {
        ReadAccessSpecification res = newRequest(objId);

        for (BACnetPropertyIdentifier prop : props)
            res.add(prop);

        return res;
    }

    public static ReadAccessSpecification newRequest(BACnetObjectIdentifier objId,
                                                     BACnetPropertyIdentifierOrUnknown prop)
    {
        ReadAccessSpecification res = newRequest(objId);
        res.add(prop);

        return res;
    }

    public static ReadAccessSpecification newRequest(BACnetObjectIdentifier objId,
                                                     BACnetPropertyIdentifierOrUnknown... props)
    {
        ReadAccessSpecification res = newRequest(objId);

        for (BACnetPropertyIdentifierOrUnknown prop : props)
            res.add(prop);

        return res;
    }

    //--//

    public void add(BACnetPropertyIdentifierOrUnknown prop)
    {
        list_of_property_references.add(BACnetPropertyReference.newInstance(prop));
    }

    public void add(BACnetPropertyIdentifierOrUnknown prop,
                    int index)
    {
        list_of_property_references.add(BACnetPropertyReference.newInstance(prop, index));
    }

    public void add(BACnetPropertyIdentifier prop)
    {
        add(prop.forRequest());
    }

    public void add(BACnetPropertyIdentifier prop,
                    int index)
    {
        add(prop.forRequest(), index);
    }
}
