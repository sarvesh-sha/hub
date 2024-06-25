/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.modbus.error;

public abstract class ModbusException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    protected ModbusException(String message)
    {
        super(message);
    }

    protected ModbusException(String message,
                              Throwable t)
    {
        super(message, t);
    }
}
