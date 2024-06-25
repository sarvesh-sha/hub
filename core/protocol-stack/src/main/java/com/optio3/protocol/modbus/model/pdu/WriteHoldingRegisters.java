/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus.model.pdu;

import com.optio3.lang.Unsigned16;
import com.optio3.protocol.modbus.pdu.ApplicationPDU;
import com.optio3.protocol.model.modbus.error.ModbusDecodingException;
import com.optio3.serialization.SerializationTag;

public class WriteHoldingRegisters extends ApplicationPDU
{
    public static class Response extends ApplicationPDU.Response
    {
        @SerializationTag(number = 1)
        public Unsigned16 startingAddress;

        @SerializationTag(number = 2)
        public Unsigned16 quantity;

        //--//

        @Override
        protected void validateAfterDecoding()
        {
            if (startingAddress == null || quantity == null)
            {
                throw ModbusDecodingException.newException("Invalid WriteHoldingRegisters.Response");
            }
        }
    }

    @SerializationTag(number = 1)
    public Unsigned16 startingAddress;

    @SerializationTag(number = 2)
    public Unsigned16 quantity;

    @SerializationTag(number = 3)
    public byte count;

    @SerializationTag(number = 4)
    public Unsigned16[] values;

    //--//

    @Override
    protected void validateAfterDecoding()
    {
        if (startingAddress == null || quantity == null || values == null)
        {
            throw ModbusDecodingException.newException("Invalid WriteHoldingRegisters");
        }

        if (count != 2 * values.length)
        {
            throw ModbusDecodingException.newException("Mot enough values for WriteHoldingRegisters: %d != %d", count, values.length);
        }
    }
}
