/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class WriteProperty extends ConfirmedServiceRequest
{
    public static final class Ack extends ConfirmedServiceResponse<WriteProperty>
    {
    }

    @SerializationTag(number = 0)
    public BACnetObjectIdentifier object_identifier;

    @SerializationTag(number = 1)
    public BACnetPropertyIdentifierOrUnknown property_identifier;

    @SerializationTag(number = 2)
    @BACnetSerializationTag(propertyIndex = true)
    public Optional<Unsigned32> property_array_index;

    @SerializationTag(number = 3)
    public Object property_value; // Depends on object_identifier & property_identifier

    @SerializationTag(number = 4)
    public Optional<Unsigned8> priority; // used only when property is commandable

    //--//

    public static WriteProperty newInstance(BACnetObjectIdentifier objId,
                                            BACnetPropertyIdentifierOrUnknown prop,
                                            Object value)
    {
        WriteProperty res = new WriteProperty();
        res.object_identifier   = objId;
        res.property_identifier = prop;
        res.property_value      = value;
        return res;
    }

    public static WriteProperty newInstance(BACnetObjectIdentifier objId,
                                            BACnetPropertyIdentifierOrUnknown prop,
                                            int index,
                                            Object value)
    {
        WriteProperty res = newInstance(objId, prop, value);
        res.property_array_index = Optional.of(Unsigned32.box(index));
        return res;
    }

    public static WriteProperty newInstance(BACnetObjectIdentifier objId,
                                            BACnetPropertyIdentifier prop,
                                            Object value)
    {
        return newInstance(objId, prop.forRequest(), value);
    }

    public static WriteProperty newInstance(BACnetObjectIdentifier objId,
                                            BACnetPropertyIdentifier prop,
                                            int index,
                                            Object value)
    {
        return newInstance(objId, prop.forRequest(), index, value);
    }
}
