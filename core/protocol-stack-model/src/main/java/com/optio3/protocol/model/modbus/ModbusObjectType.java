/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.modbus;

import org.apache.commons.lang3.StringUtils;

public enum ModbusObjectType
{
    DiscreteInput(true),
    Coil(true),
    HoldingRegister(false),
    InputRegister(false);

    //--//

    private final boolean m_isDiscrete;

    ModbusObjectType(boolean isDiscrete)
    {
        m_isDiscrete = isDiscrete;
    }

    public static ModbusObjectType parse(String value)
    {
        for (ModbusObjectType t : ModbusObjectType.values())
        {
            if (StringUtils.equalsIgnoreCase(t.name(), value))
            {
                return t;
            }
        }

        return null;
    }

    public ModbusObjectIdentifier allocateIdentifier(int num)
    {
        return new ModbusObjectIdentifier(this, num);
    }

    //--//

    public boolean isDiscrete()
    {
        return m_isDiscrete;
    }
}
