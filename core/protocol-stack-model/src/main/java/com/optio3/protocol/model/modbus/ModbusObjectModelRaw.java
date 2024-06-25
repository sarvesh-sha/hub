/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.modbus;

import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.lang.Unsigned16;

public class ModbusObjectModelRaw
{
    public Map<ModbusObjectIdentifier, Boolean>    BooleanValues = Maps.newHashMap();
    public Map<ModbusObjectIdentifier, Unsigned16> IntegerValues = Maps.newHashMap();

    //--//

    public Boolean getBooleanValue(ModbusObjectIdentifier id)
    {
        return BooleanValues.get(id);
    }

    public Integer getIntegerValueUnsigned(ModbusObjectIdentifier id)
    {
        Unsigned16 res = getIntegerValueRaw(id);
        return res != null ? res.unboxUnsigned() : null;
    }

    public Short getIntegerValueSigned(ModbusObjectIdentifier id)
    {
        Unsigned16 res = getIntegerValueRaw(id);
        return res != null ? res.unbox() : null;
    }

    public Unsigned16 getIntegerValueRaw(ModbusObjectIdentifier id)
    {
        return IntegerValues.get(id);
    }

    public void setValue(ModbusObjectIdentifier id,
                         boolean value)
    {
        BooleanValues.put(id, value);
    }

    public void setValue(ModbusObjectIdentifier id,
                         int value)
    {
        IntegerValues.put(id, Unsigned16.box(value));
    }

    public void setValue(ModbusObjectIdentifier id,
                         Unsigned16 value)
    {
        IntegerValues.put(id, value);
    }
}
