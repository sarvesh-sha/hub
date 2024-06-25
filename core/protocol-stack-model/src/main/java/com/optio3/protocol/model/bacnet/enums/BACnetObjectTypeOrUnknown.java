/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.optio3.serialization.CustomTypeDescriptor;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypeDescriptor;
import com.optio3.serialization.TypeDescriptorFactory;

@CustomTypeDescriptor(factory = BACnetObjectTypeOrUnknown.Factory.class)
public class BACnetObjectTypeOrUnknown implements Comparable<BACnetObjectTypeOrUnknown>
{
    private static final int c_unknownMarker = -1;

    public static class Factory extends TypeDescriptorFactory
    {
        @Override
        public TypeDescriptor create(Class<?> clz)
        {
            TypeDescriptor td = Reflection.getDescriptor(BACnetObjectType.class);

            return new TypeDescriptor(clz, td.size, td.kind)
            {
                @Override
                public Object fromLongValue(long value)
                {
                    BACnetObjectType objectType = BACnetObjectType.parse((byte) value);

                    if (objectType != null)
                    {
                        return objectType.forRequest();
                    }

                    return new BACnetObjectTypeOrUnknown(null, value);
                }

                @Override
                public long asLongValue(Object value)
                {
                    BACnetObjectTypeOrUnknown v = (BACnetObjectTypeOrUnknown) value;

                    return v.asLongValue();
                }
            };
        }
    }

    //--//

    private static final Supplier<Map<String, BACnetObjectType>> s_lookupByName = Suppliers.memoize(() ->
                                                                                                    {
                                                                                                        Map<String, BACnetObjectType> map = Maps.newHashMap();

                                                                                                        for (BACnetObjectType t : BACnetObjectType.values())
                                                                                                        {
                                                                                                            map.put(t.name()
                                                                                                                     .toLowerCase(), t);
                                                                                                        }

                                                                                                        return map;
                                                                                                    });

    private static final Supplier<Map<Integer, BACnetObjectType>> s_lookupByValue = Suppliers.memoize(() ->
                                                                                                      {
                                                                                                          Map<Integer, BACnetObjectType> map = Maps.newHashMap();

                                                                                                          for (BACnetObjectType t : BACnetObjectType.values())
                                                                                                          {
                                                                                                              map.put(t.getEncodingValue(), t);
                                                                                                          }

                                                                                                          return map;
                                                                                                      });

    static BACnetObjectType parseCached(String value)
    {
        if (value == null)
        {
            return null;
        }

        return s_lookupByName.get()
                             .get(value.toLowerCase());
    }

    static BACnetObjectType parseCached(int value)
    {
        return s_lookupByValue.get()
                              .get(value);
    }

    //--//

    public final BACnetObjectType value;
    public final long             unknown;

    private BACnetObjectTypeOrUnknown(BACnetObjectType value,
                                      long unknown)
    {
        this.value = value;
        this.unknown = unknown;
    }

    BACnetObjectTypeOrUnknown(BACnetObjectType value)
    {
        this.value = value;
        this.unknown = c_unknownMarker;
    }

    @JsonCreator
    private BACnetObjectTypeOrUnknown(String value)
    {
        this.value = BACnetObjectType.parse(value);
        this.unknown = this.value != null ? c_unknownMarker : Long.parseLong(value);
    }

    @JsonValue
    public String toJsonValue()
    {
        return unknown != c_unknownMarker ? Long.toString(unknown) : value.name();
    }

    public long asLongValue()
    {
        if (value != null)
        {
            return value.getEncodingValue();
        }

        return unknown;
    }

    @JsonIgnore
    public boolean isUnknown()
    {
        return unknown != c_unknownMarker;
    }

    public static BACnetObjectTypeOrUnknown parse(String value)
    {
        return new BACnetObjectTypeOrUnknown(value);
    }

    //--//

    @Override
    public int hashCode()
    {
        return Long.hashCode(asLongValue());
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        Number num = Reflection.as(o, Number.class);
        if (num != null)
        {
            return num.longValue() == asLongValue();
        }

        BACnetObjectTypeOrUnknown ot1 = Reflection.as(o, BACnetObjectTypeOrUnknown.class);
        if (ot1 != null)
        {
            return value == ot1.value && unknown == ot1.unknown;
        }

        BACnetObjectType ot2 = Reflection.as(o, BACnetObjectType.class);
        if (ot2 != null)
        {
            return value == ot2;
        }

        return false;
    }

    @Override
    public int compareTo(BACnetObjectTypeOrUnknown o)
    {
        return compare(this, o);
    }

    public static int compare(BACnetObjectTypeOrUnknown o1,
                              BACnetObjectTypeOrUnknown o2)
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

        // Sort 'devices' in front of all the other object types.
        long l1 = o1.value == BACnetObjectType.device ? -1 : o1.asLongValue();
        long l2 = o2.value == BACnetObjectType.device ? -1 : o2.asLongValue();

        return Long.compare(l1, l2);
    }

    @Override
    public String toString()
    {
        if (value != null)
        {
            return value.toString();
        }

        return Long.toString(unknown);
    }

    //--//

    public boolean hasProperty(BACnetPropertyIdentifierOrUnknown propId)
    {
        if (propId.isUnknown())
        {
            return false;
        }

        return hasProperty(propId.value);
    }

    public boolean hasProperty(BACnetPropertyIdentifier propId)
    {
        return value != null && value.hasProperty(propId);
    }

    public boolean isArrayProperty(BACnetPropertyIdentifierOrUnknown propId)
    {
        if (propId.isUnknown())
        {
            return false;
        }

        return isArrayProperty(propId.value);
    }

    public boolean isArrayProperty(BACnetPropertyIdentifier propId)
    {
        return value != null && value.isArrayProperty(propId);
    }

    public boolean isListProperty(BACnetPropertyIdentifierOrUnknown propId)
    {
        if (propId.isUnknown())
        {
            return false;
        }

        return isListProperty(propId.value);
    }

    public boolean isListProperty(BACnetPropertyIdentifier propId)
    {
        return value != null && value.isListProperty(propId);
    }
}
