/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.application;

import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.protocol.bacnet.model.enums.PduType;
import com.optio3.protocol.bacnet.model.pdu.TagContextForDecoding;
import com.optio3.protocol.bacnet.model.pdu.TagContextForEncoding;
import com.optio3.protocol.model.bacnet.error.BACnetDecodingException;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public abstract class ApplicationPDU implements AutoCloseable
{
    @SerializationTag(number = 0, bitOffset = 4, width = 4)
    public PduType pduType;

    private final InputBuffer m_inputPayload;

    public ApplicationPDU(InputBuffer buffer)
    {
        pduType = PduType.parse(this.getClass());

        if (buffer != null)
        {
            SerializationHelper.read(buffer, this);

            m_inputPayload = buffer.readNestedBlock(buffer.remainingLength());
        }
        else
        {
            m_inputPayload = null;
        }
    }

    @Override
    public void close()
    {
        if (m_inputPayload != null)
        {
            m_inputPayload.close();
        }
    }

    //--//

    public static ApplicationPDU decodeHeader(InputBuffer buffer)
    {
        byte    pduType = (byte) ((buffer.peekNextByte() >> 4) & 0x0F);
        PduType t       = PduType.parse(pduType);
        if (t == null)
        {
            throw BACnetDecodingException.newException("Encountered unknown PDU type: %s", pduType);
        }

        ApplicationPDU res = t.create(buffer);

        return res;
    }

    public void encodeHeader(OutputBuffer buffer)
    {
        SerializationHelper.write(buffer, this);
    }

    //--//

    public abstract void dispatch(ServiceContext sc);

    public ServiceCommon decodePayload()
    {
        ServiceCommon res = allocatePayload();
        if (res == null)
        {
            return null;
        }

        TagContextForDecoding context = new TagContextForDecoding(res);

        if (m_inputPayload != null)
        {
            m_inputPayload.setPosition(0);
        }

        context.decode(m_inputPayload);

        return res;
    }

    public void appendPayload(OutputBuffer output)
    {
        if (m_inputPayload != null)
        {
            m_inputPayload.copyTo(0, m_inputPayload.size(), output);
        }
    }

    public static OutputBuffer encodePayload(Object payload)
    {
        OutputBuffer          buffer  = new OutputBuffer();
        TagContextForEncoding context = new TagContextForEncoding(buffer, payload);
        context.encode();
        return buffer;
    }

    protected ServiceCommon allocatePayload()
    {
        return null;
    }
}
