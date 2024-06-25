/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.network;

import java.util.Optional;

import com.optio3.lang.Unsigned16;
import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;

public final class WhoIsRouterToNetworkPDU extends NetworkMessagePDU
{
    @SerializationTag(number = 1)
    public Optional<Unsigned16> networkNumber;

    //--//

    public WhoIsRouterToNetworkPDU()
    {
    }

    public WhoIsRouterToNetworkPDU(InputBuffer buffer)
    {
        decode(buffer);
    }

    @Override
    public void dispatch(ServiceContext sc)
    {
        sc.processNetworkMessageRequest(this);
    }
}
