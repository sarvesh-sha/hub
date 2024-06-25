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

public class ReadHoldingRegisters extends ApplicationPDU
{
    public static class Response extends ApplicationPDU.Response
    {
        @SerializationTag(number = 1)
        public byte count;

        @SerializationTag(number = 2)
        public Unsigned16[] results;

        //--//

        @Override
        protected void validateAfterDecoding()
        {
            if (results == null)
            {
                throw ModbusDecodingException.newException("Invalid ReadHoldingRegisters.Response");
            }

            if (count != 2 * results.length)
            {
                throw ModbusDecodingException.newException("Mot enough values for ReadHoldingRegisters.Response: %d != %d", count, results.length);
            }
        }
    }

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
            throw ModbusDecodingException.newException("Invalid ReadHoldingRegisters");
        }
    }
}
