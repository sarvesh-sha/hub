/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus.pdu;

import com.optio3.protocol.modbus.FunctionType;
import com.optio3.protocol.model.modbus.ModbusExceptionCode;
import com.optio3.protocol.model.modbus.error.ModbusDecodingException;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public abstract class ApplicationPDU
{
    public static abstract class Response extends ApplicationPDU
    {
    }

    public static class Error extends Response
    {
        @SerializationTag(number = 1)
        public ModbusExceptionCode exceptionCode;

        @Override
        protected void validateAfterDecoding()
        {
        }
    }

    @SerializationTag(number = 0, bitOffset = 0, width = 8)
    public byte functionType;

    //--//

    public static ApplicationPDU decodeRequest(InputBuffer buffer)
    {
        byte         functionType = buffer.peekNextByte();
        FunctionType t            = FunctionType.parse(functionType);
        if (t == null)
        {
            throw ModbusDecodingException.newException("Encountered unknown function type: %s", functionType);
        }

        ApplicationPDU apdu = t.createRequest();

        SerializationHelper.read(buffer, apdu);

        apdu.validateAfterDecoding();

        return apdu;
    }

    public static ApplicationPDU.Response decodeResponse(InputBuffer buffer)
    {
        ApplicationPDU.Response apdu;
        byte                    functionType = buffer.peekNextByte();

        if ((functionType & 0x80) == 0)
        {
            FunctionType t = FunctionType.parse(functionType);
            if (t == null)
            {
                throw ModbusDecodingException.newException("Encountered unknown function type: %s", functionType);
            }

            apdu = t.createResponse();
        }
        else
        {
            apdu = new Error();
        }

        SerializationHelper.read(buffer, apdu);

        apdu.validateAfterDecoding();

        return apdu;
    }

    public OutputBuffer encode()
    {
        OutputBuffer buffer = new OutputBuffer();

        functionType = FunctionType.parse(this.getClass())
                                   .encoding();

        SerializationHelper.write(buffer, this);

        return buffer;
    }

    protected abstract void validateAfterDecoding();
}
