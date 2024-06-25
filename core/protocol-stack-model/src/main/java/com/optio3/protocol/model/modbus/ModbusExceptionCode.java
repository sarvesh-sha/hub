/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.modbus;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum ModbusExceptionCode implements TypedBitSet.ValueGetter
{
    // @formatter:off
    ILLEGAL_FUNCTION                       (1),
    ILLEGAL_DATA_ADDRESS                   (2),
    ILLEGAL_DATA_VALUE                     (3),
    SERVER_DEVICE_FAILURE                  (4),
    ACKNOWLEDGE                            (5),
    SERVER_DEVICE_BUSY                     (6),
    MEMORY_PARITY_ERROR                    (8),
    GATEWAY_PATH_UNAVAILABLE               (10),
    GATEWAY_TARGET_DEVICE_FAILED_TO_RESPOND(11);
    // @formatter:on

    private final byte m_encoding;

    ModbusExceptionCode(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static ModbusExceptionCode parse(byte value)
    {
        for (ModbusExceptionCode t : values())
        {
            if (t.m_encoding == value)
            {
                return t;
            }
        }

        return null;
    }

    @HandlerForEncoding
    public byte encoding()
    {
        return m_encoding;
    }

    @Override
    public int getEncodingValue()
    {
        return m_encoding;
    }
}
