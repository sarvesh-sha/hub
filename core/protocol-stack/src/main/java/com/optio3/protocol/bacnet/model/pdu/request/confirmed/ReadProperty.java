/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.BACnetObjectModelMarshaller;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class ReadProperty extends ConfirmedServiceRequest
{
    public static final class Ack extends ConfirmedServiceResponse<ReadProperty> implements BACnetObjectModelMarshaller
    {
        @SerializationTag(number = 0)
        public BACnetObjectIdentifier object_identifier;

        @SerializationTag(number = 1)
        public BACnetPropertyIdentifierOrUnknown property_identifier;

        @SerializationTag(number = 2)
        @BACnetSerializationTag(propertyIndex = true)
        public Optional<Unsigned32> property_array_index;

        @SerializationTag(number = 3)
        public Object property_value; // Depends on object_identifier & property_identifier

        //--//

        @Override
        public <T extends BACnetObjectModel> T allocateObject(Class<T> clz)
        {
            Object val = object_identifier.allocateNewObject();

            return clz.cast(val);
        }

        @Override
        public void updateObjectNoLog(BACnetObjectModel target)
        {
            target.validate(object_identifier.object_type);

            target.setValueWithOptionalIndex(property_identifier, property_array_index, property_value);
        }
    }

    @SerializationTag(number = 0)
    public BACnetObjectIdentifier object_identifier;

    @SerializationTag(number = 1)
    public BACnetPropertyIdentifierOrUnknown property_identifier;

    @SerializationTag(number = 2)
    @BACnetSerializationTag(propertyIndex = true)
    public Optional<Unsigned32> property_array_index;

    //--//

    public static ReadProperty newInstance(BACnetObjectIdentifier objId,
                                           BACnetPropertyIdentifierOrUnknown prop)
    {
        ReadProperty res = new ReadProperty();
        res.object_identifier   = objId;
        res.property_identifier = prop;
        return res;
    }

    public static ReadProperty newInstance(BACnetObjectIdentifier objId,
                                           BACnetPropertyIdentifierOrUnknown prop,
                                           int index)
    {
        ReadProperty res = newInstance(objId, prop);
        res.property_array_index = Optional.of(Unsigned32.box(index));
        return res;
    }

    //--//

    public static ReadProperty newInstance(BACnetObjectIdentifier objId,
                                           BACnetPropertyIdentifier prop)
    {
        return newInstance(objId, prop.forRequest());
    }

    public static ReadProperty newInstance(BACnetObjectIdentifier objId,
                                           BACnetPropertyIdentifier prop,
                                           int index)
    {
        return newInstance(objId, prop.forRequest(), index);
    }
}
