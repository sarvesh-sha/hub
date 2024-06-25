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

public class WellKnownEquipmentClassOrCustom
{
    public final WellKnownEquipmentClass known;

    @JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
    public final int custom;

    WellKnownEquipmentClassOrCustom(WellKnownEquipmentClass pc,
                                    int custom)
    {
        this.known  = pc;
        this.custom = custom;
    }

    @JsonCreator
    public static WellKnownEquipmentClassOrCustom parse(JsonNode value)
    {
        if (value.isTextual())
        {
            return parse(value.asText());
        }

        if (value.isObject())
        {
            WellKnownEquipmentClass known  = WellKnownEquipmentClass.parse(ObjectMappers.getFieldAsText(value, "known"));
            int                     custom = ObjectMappers.getIntegerField(value, "custom");

            return known != null || custom > 0 ? new WellKnownEquipmentClassOrCustom(known, custom) : null;
        }

        return null;
    }

    public static WellKnownEquipmentClassOrCustom parse(String value)
    {
        if (value == null)
        {
            return null;
        }

        WellKnownEquipmentClass known = WellKnownEquipmentClass.parse(value);
        if (known != null)
        {
            return known.asWrapped();
        }

        try
        {
            int custom = Integer.parseInt(value);
            if (custom > 0)
            {
                return new WellKnownEquipmentClassOrCustom(null, custom);
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

    public static boolean isValid(WellKnownEquipmentClassOrCustom ec)
    {
        return ec != null && ec.asInteger() > 0;
    }

    @JsonIgnore
    public String getDisplayName()
    {
        return known != null ? known.getDisplayName() : null;
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

        WellKnownEquipmentClassOrCustom ot1 = Reflection.as(o, WellKnownEquipmentClassOrCustom.class);
        if (ot1 != null)
        {
            return known == ot1.known && custom == ot1.custom;
        }

        WellKnownEquipmentClass ot2 = Reflection.as(o, WellKnownEquipmentClass.class);
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
