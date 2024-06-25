/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.morningstar;

import java.lang.reflect.Type;

import com.optio3.protocol.model.modbus.ModbusObjectIdentifier;
import com.optio3.protocol.model.modbus.ModbusObjectType;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypedBitSet;

public class TriStarFieldModel
{
    public final Type type;

    public final ModbusObjectIdentifier       pdu;
    public final ModbusObjectIdentifier       pduLow;
    public final boolean                      signed;
    public final Class<? extends Enum>        enumClass;
    public final Class<? extends TypedBitSet> bitsetClass;

    public final float fixedScaling;

    public final boolean voltageScaling;
    public final boolean currentScaling;

    public TriStarFieldModel(Type type,
                             int pdu,
                             int length,
                             boolean signed,
                             float fixedScaling,
                             boolean voltageScaling,
                             boolean currentScaling)
    {
        Class<?> clz = Reflection.getRawType(type);

        boolean isDiscrete = clz == boolean.class;
        boolean isEnum     = clz.isEnum();
        boolean isBitSet   = Reflection.isSubclassOf(TypedBitSet.class, clz);

        this.type = type;

        this.signed = signed;

        this.fixedScaling = fixedScaling;
        this.voltageScaling = voltageScaling;
        this.currentScaling = currentScaling;

        if (isDiscrete)
        {
            this.pdu = ModbusObjectType.DiscreteInput.allocateIdentifier(pdu);
            this.pduLow = null;
        }
        else if (isEnum)
        {
            this.pdu = ModbusObjectType.HoldingRegister.allocateIdentifier(pdu);
            this.pduLow = null;
        }
        else if (length == 2)
        {
            this.pdu = ModbusObjectType.HoldingRegister.allocateIdentifier(pdu);
            this.pduLow = ModbusObjectType.HoldingRegister.allocateIdentifier(pdu + 1);
        }
        else
        {
            this.pdu = ModbusObjectType.HoldingRegister.allocateIdentifier(pdu);
            this.pduLow = null;
        }

        this.enumClass = isEnum ? Reflection.getRawType(type) : null;
        this.bitsetClass = isBitSet ? Reflection.getRawType(type) : null;
    }
}
