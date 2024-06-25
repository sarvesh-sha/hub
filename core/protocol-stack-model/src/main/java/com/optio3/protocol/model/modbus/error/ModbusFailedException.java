/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.modbus.error;

import com.optio3.protocol.model.modbus.ModbusExceptionCode;

public class ModbusFailedException extends ModbusException
{
    private static final long serialVersionUID = 1L;

    public final ModbusExceptionCode code;

    public ModbusFailedException(ModbusExceptionCode code)
    {
        this(code, null);
    }

    public ModbusFailedException(ModbusExceptionCode code,
                                 Throwable t)
    {
        super(String.format("Failed with code %s", code), t);

        this.code = code;
    }
}
