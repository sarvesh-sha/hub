/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.epsolar;

import java.lang.reflect.Type;

import com.optio3.protocol.model.modbus.ModbusObjectIdentifier;
import com.optio3.protocol.model.modbus.ModbusObjectType;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypedBitSet;

public class EpSolarFieldModel
{
    public final Type type;

    public final ModbusObjectIdentifier       pdu;
    public final ModbusObjectIdentifier       pduHigh;
    public final boolean                      signed;
    public final boolean                      writable;
    public final Class<? extends Enum>        enumClass;
    public final Class<? extends TypedBitSet> bitsetClass;

    public final float fixedScaling;

    public EpSolarFieldModel(Type type,
                             int pdu,
                             int length,
                             boolean signed,
                             boolean writable,
                             float fixedScaling)
    {
        Class<?> clz = Reflection.getRawType(type);

        boolean isDiscrete = clz == boolean.class;
        boolean isEnum     = clz.isEnum();
        boolean isBitSet   = Reflection.isSubclassOf(TypedBitSet.class, clz);

        this.type = type;

        this.signed = signed;
        this.writable = writable;

        this.fixedScaling = fixedScaling;

        final ModbusObjectType nonDiscrete = writable ? ModbusObjectType.HoldingRegister : ModbusObjectType.InputRegister;

        if (isDiscrete)
        {
            this.pdu = ModbusObjectType.DiscreteInput.allocateIdentifier(pdu);
            this.pduHigh = null;
        }
        else if (isEnum)
        {
            this.pdu = nonDiscrete.allocateIdentifier(pdu);
            this.pduHigh = null;
        }
        else if (length == 2)
        {
            this.pdu = nonDiscrete.allocateIdentifier(pdu);
            this.pduHigh = nonDiscrete.allocateIdentifier(pdu + 1);
        }
        else
        {
            this.pdu = nonDiscrete.allocateIdentifier(pdu);
            this.pduHigh = null;
        }

        this.enumClass = isEnum ? Reflection.getRawType(type) : null;
        this.bitsetClass = isBitSet ? Reflection.getRawType(type) : null;
    }
}
