/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.modbus.error;

public class ModbusDecodingException extends ModbusException
{
    private static final long serialVersionUID = 1L;

    public ModbusDecodingException(String message)
    {
        super(message);
    }

    public ModbusDecodingException(String message,
                                   Throwable t)
    {
        super(message, t);
    }

    public static ModbusDecodingException newException(String fmt,
                                                       Object... args)
    {
        return new ModbusDecodingException(String.format(fmt, args));
    }
}
