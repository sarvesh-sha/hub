/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.optio3.cloud.client.SwaggerTypeReplacement;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationTag;
import org.apache.commons.lang3.StringUtils;

@SwaggerTypeReplacement(targetElement = String.class)
public final class BACnetObjectIdentifier implements Comparable<BACnetObjectIdentifier>
{
    public static int MIN_INSTANCE_NUMBER = 0;
    public static int MAX_INSTANCE_NUMBER = (1 << 22) - 1;

    @SerializationTag(number = 0, width = 10, bitOffset = 22)
    public BACnetObjectTypeOrUnknown object_type;

    @SerializationTag(number = 0, width = 22, bitOffset = 0)
    public Unsigned32 instance_number;

    //--//

    public BACnetObjectIdentifier()
    {
    }

    public BACnetObjectIdentifier(BACnetObjectType objectType,
                                  int instanceNumber)
    {
        object_type = objectType.forRequest();
        instance_number = Unsigned32.box(instanceNumber);
    }

    @JsonCreator
    public BACnetObjectIdentifier(String value)
    {
        String[] parts = StringUtils.split(value, '/');

        object_type = BACnetObjectTypeOrUnknown.parse(parts[0]);
        instance_number = Unsigned32.box(Long.parseLong(parts[1]));
    }

    @JsonValue
    public String toJsonValue()
    {
        return object_type.toJsonValue() + "/" + instance_number;
    }

    public BACnetObjectModel allocateNewObject()
    {
        return object_type.value.allocateNewObject(this);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        BACnetObjectIdentifier that = Reflection.as(o, BACnetObjectIdentifier.class);
        if (that == null)
        {
            return false;
        }

        if (object_type != null ? !object_type.equals(that.object_type) : that.object_type != null)
        {
            return false;
        }

        return instance_number != null ? instance_number.equals(that.instance_number) : that.instance_number == null;
    }

    @Override
    public int hashCode()
    {
        int result = object_type != null ? object_type.hashCode() : 0;
        result = 31 * result + (instance_number != null ? instance_number.hashCode() : 0);
        return result;
    }

    @Override
    public int compareTo(BACnetObjectIdentifier o)
    {
        return compare(this, o);
    }

    public static int compare(BACnetObjectIdentifier o1,
                              BACnetObjectIdentifier o2)
    {
        if (o1 == o2)
        {
            return 0;
        }

        if (o1 == null)
        {
            return 1;
        }

        if (o2 == null)
        {
            return -1;
        }

        int diff = BACnetObjectTypeOrUnknown.compare(o1.object_type, o2.object_type);
        if (diff == 0)
        {
            diff = Unsigned32.compare(o1.instance_number, o2.instance_number);
        }

        return diff;
    }

    @Override
    public String toString()
    {
        return String.format("ObjId:%s:%d", object_type, instance_number.unbox());
    }
}
