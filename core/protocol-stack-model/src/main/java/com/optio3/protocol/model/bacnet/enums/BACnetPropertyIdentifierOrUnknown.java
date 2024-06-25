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

@CustomTypeDescriptor(factory = BACnetPropertyIdentifierOrUnknown.Factory.class)
public class BACnetPropertyIdentifierOrUnknown implements Comparable<BACnetPropertyIdentifierOrUnknown>
{
    private static final int c_unknownMarker = -1;

    public static class Factory extends TypeDescriptorFactory
    {

        @Override
        public TypeDescriptor create(Class<?> clz)
        {
            TypeDescriptor td = Reflection.getDescriptor(BACnetPropertyIdentifier.class);

            return new TypeDescriptor(clz, td.size, td.kind)
            {
                @Override
                public Object fromLongValue(long value)
                {
                    BACnetPropertyIdentifier objectType = BACnetPropertyIdentifier.parse((short) value);

                    if (objectType != null)
                    {
                        return objectType.forRequest();
                    }

                    return new BACnetPropertyIdentifierOrUnknown(null, value);
                }

                @Override
                public long asLongValue(Object value)
                {
                    BACnetPropertyIdentifierOrUnknown v = (BACnetPropertyIdentifierOrUnknown) value;

                    return v.asLongValue();
                }
            };
        }
    }

    //--//

    private static final Supplier<Map<String, BACnetPropertyIdentifier>> s_lookupByName = Suppliers.memoize(() ->
                                                                                                            {
                                                                                                                Map<String, BACnetPropertyIdentifier> map = Maps.newHashMap();

                                                                                                                for (BACnetPropertyIdentifier t : BACnetPropertyIdentifier.values())
                                                                                                                {
                                                                                                                    map.put(t.name()
                                                                                                                             .toLowerCase(), t);
                                                                                                                }

                                                                                                                return map;
                                                                                                            });

    private static final Supplier<Map<Integer, BACnetPropertyIdentifier>> s_lookupByValue = Suppliers.memoize(() ->
                                                                                                              {
                                                                                                                  Map<Integer, BACnetPropertyIdentifier> map = Maps.newHashMap();

                                                                                                                  for (BACnetPropertyIdentifier t : BACnetPropertyIdentifier.values())
                                                                                                                  {
                                                                                                                      map.put(t.getEncodingValue(), t);
                                                                                                                  }

                                                                                                                  return map;
                                                                                                              });

    static BACnetPropertyIdentifier parseCached(String value)
    {
        if (value == null)
        {
            return null;
        }

        return s_lookupByName.get()
                             .get(value.toLowerCase());
    }

    static BACnetPropertyIdentifier parseCached(int value)
    {
        return s_lookupByValue.get()
                              .get(value);
    }

    //--//

    public final BACnetPropertyIdentifier value;
    public final long                     unknown;

    private BACnetPropertyIdentifierOrUnknown(BACnetPropertyIdentifier value,
                                              long unknown)
    {
        this.value = value;
        this.unknown = unknown;
    }

    BACnetPropertyIdentifierOrUnknown(BACnetPropertyIdentifier value)
    {
        this.value = value;
        this.unknown = c_unknownMarker;
    }

    @JsonCreator
    public static BACnetPropertyIdentifierOrUnknown parse(String value)
    {
        BACnetPropertyIdentifier parsedValue = BACnetPropertyIdentifier.parse(value);
        if (parsedValue != null)
        {
            return parsedValue.forRequest();
        }

        return new BACnetPropertyIdentifierOrUnknown(null, Long.parseLong(value));
    }

    @JsonValue
    public String toJsonValue()
    {
        return isUnknown() ? Long.toString(unknown) : value.name();
    }

    @JsonIgnore
    public boolean isUnknown()
    {
        return unknown != c_unknownMarker;
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

        BACnetPropertyIdentifierOrUnknown id1 = Reflection.as(o, BACnetPropertyIdentifierOrUnknown.class);
        if (id1 != null)
        {
            return value == id1.value && unknown == id1.unknown;
        }

        BACnetPropertyIdentifier id2 = Reflection.as(o, BACnetPropertyIdentifier.class);
        if (id2 != null)
        {
            return value == id2;
        }

        return false;
    }

    @Override
    public int compareTo(BACnetPropertyIdentifierOrUnknown o)
    {
        return Long.compare(this.asLongValue(), o.asLongValue());
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

    public boolean equals(BACnetPropertyIdentifierOrUnknown obj)
    {
        return obj != null && value == obj.value && unknown == obj.unknown;
    }

    public boolean equals(BACnetPropertyIdentifier obj)
    {
        return obj != null && value == obj;
    }

    //--//

    protected long asLongValue()
    {
        if (value != null)
        {
            return value.getEncodingValue();
        }

        return unknown;
    }
}
