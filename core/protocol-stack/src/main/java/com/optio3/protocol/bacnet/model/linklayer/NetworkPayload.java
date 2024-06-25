/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.linklayer;

import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.protocol.bacnet.model.pdu.NetworkPDU;
import com.optio3.stream.InputBuffer;

public abstract class NetworkPayload extends BaseVirtualLinkLayer
{
    private InputBuffer m_payload;

    //--//

    @Override
    protected void decodePayload(InputBuffer payload)
    {
        m_payload = payload.readNestedBlock(payload.remainingLength());
    }

    public NetworkPDU decodePayload()
    {
        return NetworkPDU.decode(m_payload);
    }

    //--//

    protected void dispatchInner(ServiceContext sc)
    {
        try (NetworkPDU npdu = decodePayload())
        {
            sc.processNetworkRequest(npdu);
        }
    }
}
