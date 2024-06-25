/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus;

import com.optio3.protocol.modbus.model.pdu.ReadCoils;
import com.optio3.protocol.modbus.model.pdu.ReadDeviceIdentification;
import com.optio3.protocol.modbus.model.pdu.ReadDiscreteInputs;
import com.optio3.protocol.modbus.model.pdu.ReadHoldingRegisters;
import com.optio3.protocol.modbus.model.pdu.ReadInputRegisters;
import com.optio3.protocol.modbus.model.pdu.WriteHoldingRegisters;
import com.optio3.protocol.modbus.pdu.ApplicationPDU;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.Reflection;

public enum FunctionType
{
    // @formatter:off
    Read_Coils                (0x01, ReadCoils               .class, ReadCoils               .Response.class),
    Read_Discrete_Inputs      (0x02, ReadDiscreteInputs      .class, ReadDiscreteInputs      .Response.class),
    Read_Holding_Registers    (0x03, ReadHoldingRegisters    .class, ReadHoldingRegisters    .Response.class),
    Write_Holding_Registers   (0x10, WriteHoldingRegisters   .class, WriteHoldingRegisters   .Response.class),
    Read_Input_Registers      (0x04, ReadInputRegisters      .class, ReadInputRegisters      .Response.class),
    Read_Device_Identification(0x2B, ReadDeviceIdentification.class, ReadDeviceIdentification.Response.class);
    // @formatter:on

    private final byte                                     m_encoding;
    private final Class<? extends ApplicationPDU>          m_clzRequest;
    private final Class<? extends ApplicationPDU.Response> m_clzResponse;

    FunctionType(int encoding,
                 Class<? extends ApplicationPDU> clzRequest,
                 Class<? extends ApplicationPDU.Response> clzResponse)
    {
        m_encoding = (byte) encoding;
        m_clzRequest = clzRequest;
        m_clzResponse = clzResponse;
    }

    @HandlerForDecoding
    public static FunctionType parse(byte value)
    {
        for (FunctionType t : values())
        {
            if (t.m_encoding == value)
            {
                return t;
            }
        }

        return null;
    }

    public static FunctionType parse(Class<? extends ApplicationPDU> value)
    {
        for (FunctionType t : values())
        {
            if (t.m_clzRequest == value || t.m_clzResponse == value)
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

    public Class<? extends ApplicationPDU> factoryForRequest()
    {
        return m_clzRequest;
    }

    public Class<? extends ApplicationPDU.Response> factoryForResponse()
    {
        return m_clzResponse;
    }

    public ApplicationPDU createRequest()
    {
        return Reflection.newInstance(m_clzRequest);
    }

    public ApplicationPDU.Response createResponse()
    {
        return Reflection.newInstance(m_clzResponse);
    }
}
