/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.serialization.Reflection;
import com.optio3.util.function.CallableWithoutException;

public class MetadataField<T>
{
    public static final TypeReference<List<String>> TypeRef_listOfStrings = new TypeReference<>()
    {
    };

    public static final TypeReference<Set<String>> TypeRef_setOfStrings = new TypeReference<>()
    {
    };

    public static final TypeReference<Map<String, String>> TypeRef_mapOfStrings = new TypeReference<>()
    {
    };

    //--//

    private final String                      key;
    private final Class<T>                    clz;
    private final TypeReference<T>            typeRef;
    private final CallableWithoutException<T> factory;

    public MetadataField(String key,
                         Class<T> clz)
    {
        this(key, clz, null);
    }

    public MetadataField(String key,
                         Class<T> clz,
                         CallableWithoutException<T> factory)
    {
        this.key     = key;
        this.clz     = clz;
        this.typeRef = null;
        this.factory = factory;
    }

    public MetadataField(String key,
                         TypeReference<T> typeRef)
    {
        this(key, typeRef, null);
    }

    public MetadataField(String key,
                         TypeReference<T> typeRef,
                         CallableWithoutException<T> factory)
    {
        this.key     = key;
        this.clz     = null;
        this.typeRef = typeRef;
        this.factory = factory;
    }

    public boolean isPresent(MetadataMap map)
    {
        return map.contains(key);
    }

    public T get(MetadataMap map)
    {
        T res = getImpl(map);
        if (res == null && factory != null)
        {
            res = factory.call();
        }

        return res;
    }

    private T getImpl(MetadataMap map)
    {
        if (typeRef != null)
        {
            return map.getObject(key, typeRef);
        }
        else
        {
            Object val;

            if (clz == String.class)
            {
                val = map.getString(key);
            }
            else if (clz == Boolean.class)
            {
                val = map.getBooleanOrDefault(key, false);
            }
            else if (clz == Integer.class)
            {
                val = map.getIntOrDefault(key, 0);
            }
            else if (clz == Double.class)
            {
                val = map.getDoubleOrDefault(key, 0.0);
            }
            else if (clz == ZonedDateTime.class)
            {
                val = map.getDateTime(key);
            }
            else
            {
                val = map.getObject(key, clz);
            }

            return clz.cast(val);
        }
    }

    public T getOrDefault(MetadataMap map,
                          T defaultValue)
    {
        if (map.contains(key))
        {
            return get(map);
        }

        return defaultValue;
    }

    public boolean put(MetadataMap map,
                       T val)
    {
        if (clz == String.class)
        {
            return map.putStringOrRemoveIfNull(key, (String) val);
        }

        if (clz == Boolean.class)
        {
            return map.putBooleanOrRemoveIfNull(key, (Boolean) val);
        }

        if (clz == Integer.class)
        {
            return map.putIntOrRemoveIfNull(key, (Integer) val);
        }

        if (clz == ZonedDateTime.class)
        {
            return map.putDateTimeOrRemoveIfNull(key, (ZonedDateTime) val);
        }

        if (factory != null)
        {
            Collection<?> coll = Reflection.as(val, Collection.class);
            if (coll != null && coll.isEmpty())
            {
                val = null;
            }
        }

        return map.putObjectOrRemoveIfNull(key, val);
    }

    public void remove(MetadataMap map)
    {
        map.remove(key);
    }
}
