/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.util.EnumSet;
import java.util.Set;

import com.google.common.collect.Sets;

public enum JsonConnectionCapability
{
    BinaryStream("TRANSPORT:BINARY"),
    CompressedStream("TRANSPORT:COMPRESSED"),
    UDPv1("TRANSPORT:UDPv1");

    private final String m_value;

    JsonConnectionCapability(String value)
    {
        m_value = value;
    }

    public static JsonConnectionCapability parse(String text)
    {
        for (JsonConnectionCapability value : values())
        {
            if (value.m_value.equals(text))
            {
                return value;
            }
        }

        return null;
    }

    public static EnumSet<JsonConnectionCapability> decode(Set<String> input)
    {
        EnumSet<JsonConnectionCapability> set = EnumSet.noneOf(JsonConnectionCapability.class);

        if (input != null)
        {
            for (String text : input)
            {
                JsonConnectionCapability value = parse(text);
                if (value != null)
                {
                    set.add(value);
                }
            }
        }

        return set;
    }

    public static Set<String> encode(EnumSet<JsonConnectionCapability> input)
    {
        if (input == null || input.isEmpty())
        {
            return null;
        }

        Set<String> set = Sets.newHashSet();
        for (JsonConnectionCapability value : input)
        {
            set.add(value.m_value);
        }

        return set;
    }
}
