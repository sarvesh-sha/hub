/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus.model.pdu;

import java.util.BitSet;

import com.optio3.lang.Unsigned16;
import com.optio3.protocol.modbus.pdu.ApplicationPDU;
import com.optio3.protocol.model.modbus.error.ModbusDecodingException;
import com.optio3.serialization.SerializationTag;

public class ReadCoils extends ApplicationPDU
{
    public static class Response extends ApplicationPDU.Response
    {
        @SerializationTag(number = 1)
        public byte count;

        @SerializationTag(number = 2)
        public byte[] results;

        //--//

        @Override
        protected void validateAfterDecoding()
        {
            if (results == null)
            {
                throw ModbusDecodingException.newException("Invalid ReadCoils.Response");
            }

            if (count != results.length)
            {
                throw ModbusDecodingException.newException("Mot enough values for ReadCoils.Response: %d != %d", count, results.length);
            }
        }

        //--//

        public BitSet asBits()
        {
            BitSet res = new BitSet();

            for (int i = 0; i < count; i++)
            {
                byte word    = results[i / 8];
                int  bit     = i % 8;
                byte bitMask = (byte) (1 << bit);

                if ((word & bitMask) != 0)
                {
                    res.set(i);
                }
            }

            return res;
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
            throw ModbusDecodingException.newException("Invalid ReadCoils");
        }
    }
}
