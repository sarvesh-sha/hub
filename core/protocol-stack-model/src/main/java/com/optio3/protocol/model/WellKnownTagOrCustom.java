/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;

public class WellKnownTagOrCustom
{
    public final WellKnownTag known;
    public final String       custom;

    WellKnownTagOrCustom(WellKnownTag tag,
                         String custom)
    {
        this.known  = tag;
        this.custom = custom;
    }

    @JsonCreator
    public static WellKnownTagOrCustom parse(JsonNode value)
    {
        if (value.isTextual())
        {
            return parse(value.asText());
        }

        if (value.isObject())
        {
            WellKnownTag known  = WellKnownTag.parse(ObjectMappers.getFieldAsText(value, "known"));
            String       custom = ObjectMappers.getFieldAsText(value, "custom");

            return known != null || custom != null ? new WellKnownTagOrCustom(known, custom) : null;
        }

        return null;
    }

    public static WellKnownTagOrCustom parse(String value)
    {
        if (value == null)
        {
            return null;
        }

        WellKnownTag known  = WellKnownTag.parse(value);
        String       custom = known == null ? value : null;

        return new WellKnownTagOrCustom(known, custom);
    }

    @JsonIgnore
    public String asString()
    {
        return known != null ? known.name() : custom;
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        String str = Reflection.as(o, String.class);
        if (str != null)
        {
            return str.equals(asString());
        }

        WellKnownTagOrCustom ot1 = Reflection.as(o, WellKnownTagOrCustom.class);
        if (ot1 != null)
        {
            return known == ot1.known && Objects.equals(custom, ot1.custom);
        }

        WellKnownTag ot2 = Reflection.as(o, WellKnownTag.class);
        if (ot2 != null)
        {
            return known == ot2;
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return asString().hashCode();
    }

    @Override
    public String toString()
    {
        return asString();
    }
}
