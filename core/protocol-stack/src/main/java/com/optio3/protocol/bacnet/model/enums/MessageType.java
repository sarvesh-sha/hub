/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.enums;

import com.optio3.protocol.bacnet.model.pdu.network.IAmRouterToNetworkPDU;
import com.optio3.protocol.bacnet.model.pdu.network.ICouldBeRouterToNetworkPDU;
import com.optio3.protocol.bacnet.model.pdu.network.NetworkMessagePDU;
import com.optio3.protocol.bacnet.model.pdu.network.NetworkNumberIsPDU;
import com.optio3.protocol.bacnet.model.pdu.network.RejectMessageToNetworkPDU;
import com.optio3.protocol.bacnet.model.pdu.network.RouterAvailableToNetworkPDU;
import com.optio3.protocol.bacnet.model.pdu.network.RouterBusyToNetworkPDU;
import com.optio3.protocol.bacnet.model.pdu.network.WhatIsNetworkNumberPDU;
import com.optio3.protocol.bacnet.model.pdu.network.WhoIsRouterToNetworkPDU;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.Reflection;
import com.optio3.stream.InputBuffer;

public enum MessageType
{
    // @formatter:off
    Who_Is_Router_To_Network        (0x00, WhoIsRouterToNetworkPDU.class),
    I_Am_Router_To_Network          (0x01, IAmRouterToNetworkPDU.class),
    I_Could_Be_Router_To_Network    (0x02, ICouldBeRouterToNetworkPDU.class),
    Reject_Message_To_Network       (0x03, RejectMessageToNetworkPDU.class),
    Router_Busy_To_Network          (0x04, RouterBusyToNetworkPDU.class),
    Router_Available_To_Network     (0x05, RouterAvailableToNetworkPDU.class),
    Initialize_Routing_Table        (0x06, null),
    Initialize_Routing_Table_Ack    (0x07, null),
    Establish_Connection_To_Network (0x08, null),
    Disconnect_Connection_To_Network(0x09, null),
    Challenge_Request               (0x0A, null),
    Security_Payload                (0x0B, null),
    Security_Response               (0x0C, null),
    Request_Key_Update              (0x0D, null),
    Update_Key_Set                  (0x0E, null),
    Update_Distribution_Key         (0x0F, null),
    Request_Master_Key              (0x10, null),
    Set_Master_Key                  (0x11, null),
    What_Is_Network_Number          (0x12, WhatIsNetworkNumberPDU.class),
    Network_Number_Is               (0x13, NetworkNumberIsPDU.class);
    // @formatter:on

    private final byte                                                                            m_encoding;
    private final Class<? extends com.optio3.protocol.bacnet.model.pdu.network.NetworkMessagePDU> m_clz;

    MessageType(int encoding,
                Class<? extends NetworkMessagePDU> clz)
    {
        m_encoding = (byte) encoding;
        m_clz      = clz;
    }

    @HandlerForDecoding
    public static MessageType parse(byte value)
    {
        for (MessageType t : values())
        {
            if (t.m_encoding == value)
            {
                return t;
            }
        }

        return null;
    }

    public static MessageType parse(Class<? extends NetworkMessagePDU> value)
    {
        for (MessageType t : values())
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

    public Class<? extends NetworkMessagePDU> factory()
    {
        return m_clz;
    }

    public NetworkMessagePDU create(InputBuffer buffer)
    {
        return Reflection.newInstance(m_clz, buffer);
    }
}
