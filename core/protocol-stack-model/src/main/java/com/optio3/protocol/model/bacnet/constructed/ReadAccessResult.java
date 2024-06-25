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
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.BACnetObjectModelMarshaller;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class ReadAccessResult extends Sequence implements BACnetObjectModelMarshaller
{
    public static final class Values extends Sequence
    {
        @SerializationTag(number = 2)
        public BACnetPropertyIdentifierOrUnknown property_identifier;

        @SerializationTag(number = 3)
        @BACnetSerializationTag(propertyIndex = true)
        public Optional<Unsigned32> property_array_index;

        @SerializationTag(number = 4)
        @BACnetSerializationTag(choiceSet = "read-result")
        public Object property_value;

        @SerializationTag(number = 5)
        @BACnetSerializationTag(choiceSet = "read-result")
        public BACnetError property_access_error;

        @Override
        public String toString()
        {
            if (property_array_index != null && property_array_index.isPresent())
            {
                return String.format("%s[%s] = %s", property_array_index.get(), property_identifier, property_value);
            }
            else
            {
                return String.format("%s = %s", property_identifier, property_value);
            }
        }
    }

    @SerializationTag(number = 0)
    public BACnetObjectIdentifier object_identifier;

    @SerializationTag(number = 1)
    public List<Values> list_of_results = Lists.newArrayList();

    //--//

    public Values add(BACnetPropertyIdentifierOrUnknown prop)
    {
        Values value = new Values();
        value.property_identifier = prop;

        list_of_results.add(value);
        return value;
    }

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

        for (Values val : list_of_results)
        {
            if (val.property_access_error != null)
            {
                continue;
            }

            target.setValueWithOptionalIndex(val.property_identifier, val.property_array_index, val.property_value);
        }
    }
}
