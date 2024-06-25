/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.modbus;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

public final class ModbusObjectIdentifier implements Comparable<ModbusObjectIdentifier>
{
    public ModbusObjectType type;
    public int              number;

    //--//

    public ModbusObjectIdentifier()
    {
    }

    public ModbusObjectIdentifier(ModbusObjectType type,
                                  int number)
    {
        this.type = type;
        this.number = number;
    }

    @JsonCreator
    public ModbusObjectIdentifier(String value)
    {
        String[] parts = StringUtils.split(value, '/');

        type = ModbusObjectType.parse(parts[0]);
        number = Integer.valueOf(parts[1]);
    }

    @JsonValue
    public String toJsonValue()
    {
        return type.name() + "/" + number;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        ModbusObjectIdentifier that = Reflection.as(o, ModbusObjectIdentifier.class);
        if (that == null)
        {
            return false;
        }

        return type == that.type && number == that.number;
    }

    @Override
    public int hashCode()
    {
        return number;
    }

    @Override
    public int compareTo(ModbusObjectIdentifier o)
    {
        return compare(this, o);
    }

    public static int compare(ModbusObjectIdentifier o1,
                              ModbusObjectIdentifier o2)
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

        int diff = Integer.compare(o1.type.ordinal(), o2.type.ordinal());
        if (diff == 0)
        {
            diff = Integer.compare(o1.number, o2.number);
        }

        return diff;
    }

    @Override
    public String toString()
    {
        return String.format("ObjId:%s:%d", type, number);
    }
}
