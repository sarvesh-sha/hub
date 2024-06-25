/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;

public class WellKnownPointClassOrCustom
{
    public final WellKnownPointClass known;

    @JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
    public final int custom;

    WellKnownPointClassOrCustom(WellKnownPointClass pc,
                                int custom)
    {
        this.known  = pc;
        this.custom = custom;
    }

    @JsonCreator
    public static WellKnownPointClassOrCustom parse(JsonNode value)
    {
        if (value.isTextual())
        {
            return parse(value.asText());
        }

        if (value.isObject())
        {
            WellKnownPointClass known  = WellKnownPointClass.parse(ObjectMappers.getFieldAsText(value, "known"));
            int                 custom = ObjectMappers.getIntegerField(value, "custom");

            return known != null || custom > 0 ? new WellKnownPointClassOrCustom(known, custom) : null;
        }

        return null;
    }

    public static WellKnownPointClassOrCustom parse(String value)
    {
        if (value == null)
        {
            return null;
        }

        WellKnownPointClass known = WellKnownPointClass.parse(value);
        if (known != null)
        {
            return known.asWrapped();
        }

        try
        {
            int custom = Integer.parseInt(value);
            if (custom > 0)
            {
                return new WellKnownPointClassOrCustom(null, custom);
            }
        }
        catch (NumberFormatException e)
        {
            // Invalid encoding.
        }

        return null;
    }

    @JsonIgnore
    public int asInteger()
    {
        return known != null ? known.getId() : custom;
    }

    public static boolean isValid(WellKnownPointClassOrCustom pc)
    {
        return pc != null && pc.asInteger() > 0;
    }

    //--//

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
            return num.intValue() == asInteger();
        }

        WellKnownPointClassOrCustom ot1 = Reflection.as(o, WellKnownPointClassOrCustom.class);
        if (ot1 != null)
        {
            return known == ot1.known && custom == ot1.custom;
        }

        WellKnownPointClass ot2 = Reflection.as(o, WellKnownPointClass.class);
        if (ot2 != null)
        {
            return known == ot2;
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return asInteger();
    }

    @Override
    public String toString()
    {
        if (known != null)
        {
            return known.name();
        }

        return Integer.toString(custom);
    }
}
