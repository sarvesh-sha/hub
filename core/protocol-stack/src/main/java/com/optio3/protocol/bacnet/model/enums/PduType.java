/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.enums;

import com.optio3.protocol.bacnet.model.pdu.application.AbortPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ApplicationPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ComplexAckPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ConfirmedRequestPDU;
import com.optio3.protocol.bacnet.model.pdu.application.ErrorPDU;
import com.optio3.protocol.bacnet.model.pdu.application.RejectPDU;
import com.optio3.protocol.bacnet.model.pdu.application.SegmentAckPDU;
import com.optio3.protocol.bacnet.model.pdu.application.SimpleAckPDU;
import com.optio3.protocol.bacnet.model.pdu.application.UnconfirmedRequestPDU;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.Reflection;
import com.optio3.stream.InputBuffer;

public enum PduType
{
    // @formatter:off
    Confirmed_Service_Request  (0x00, ConfirmedRequestPDU.class),
    Unconfirmed_Service_Request(0x01, UnconfirmedRequestPDU.class),
    SimpleACK                  (0x02, SimpleAckPDU.class),
    ComplexACK                 (0x03, ComplexAckPDU.class),
    SegmentACK                 (0x04, SegmentAckPDU.class),
    Error                      (0x05, ErrorPDU.class),
    Reject                     (0x06, RejectPDU.class),
    Abort                      (0x07, AbortPDU.class);
    // @formatter:on

    private final byte                            m_encoding;
    private final Class<? extends ApplicationPDU> m_clz;

    PduType(int encoding,
            Class<? extends ApplicationPDU> clz)
    {
        m_encoding = (byte) encoding;
        m_clz      = clz;
    }

    @HandlerForDecoding
    public static PduType parse(byte value)
    {
        for (PduType t : values())
        {
            if (t.m_encoding == value)
            {
                return t;
            }
        }

        return null;
    }

    public static PduType parse(Class<? extends ApplicationPDU> value)
    {
        for (PduType t : values())
        {
            if (t.m_clz == value)
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

    public Class<? extends ApplicationPDU> factory()
    {
        return m_clz;
    }

    public ApplicationPDU create(InputBuffer buffer)
    {
        return Reflection.newInstance(m_clz, buffer);
    }
}
