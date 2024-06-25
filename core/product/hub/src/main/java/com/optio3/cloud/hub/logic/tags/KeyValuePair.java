/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.tags;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.optio3.collection.Memoizer;
import com.optio3.serialization.Reflection;
import com.optio3.stream.MemoryMappedHeap;
import org.apache.commons.lang3.StringUtils;

class KeyValuePair
{
    final   String key;
    final   String value;
    private int    m_hashCode;

    KeyValuePair(String key,
                 String value)
    {
        this.key   = key;
        this.value = value;
    }

    @Override
    public int hashCode()
    {
        if (m_hashCode == 0)
        {
            m_hashCode = hashCodeLowercase(key) + 31 * hashCodeLowercase(value);
        }

        return m_hashCode;
    }

    @Override
    public boolean equals(Object o)
    {
        KeyValuePair that = Reflection.as(o, KeyValuePair.class);
        if (that == null)
        {
            return false;
        }

        return StringUtils.equalsIgnoreCase(key, that.key) && StringUtils.equalsIgnoreCase(value, that.value);
    }

    @Override
    public String toString()
    {
        return "KeyValuePair{" + "key='" + key + '\'' + ", value='" + value + '\'' + '}';
    }

    //--//

    void serialize(DataOutputStream stream) throws
                                            IOException
    {
        MemoryMappedHeap.serializeString(stream, key);
        MemoryMappedHeap.serializeString(stream, value);
    }

    static KeyValuePair deserialize(Memoizer cache,
                                    DataInputStream stream) throws
                                                            IOException
    {
        String key   = cache.intern(MemoryMappedHeap.deserializeString(stream));
        String value = cache.intern(MemoryMappedHeap.deserializeString(stream));

        return new KeyValuePair(key, value);
    }

    //--//

    private static int hashCodeLowercase(String s)
    {
        return s != null ? s.toLowerCase()
                            .hashCode() : 1; // Avoid using zero, since we cache results.
    }
}
