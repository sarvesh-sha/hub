/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.network;

import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;

public final class ICouldBeRouterToNetworkPDU extends NetworkMessagePDU
{
    @SerializationTag(number = 1)
    public Unsigned16 networkNumber;

    @SerializationTag(number = 2)
    public Unsigned8 performanceIndex;

    //--//

    public ICouldBeRouterToNetworkPDU(InputBuffer buffer)
    {
        decode(buffer);
    }

    @Override
    public void dispatch(ServiceContext sc)
    {
        sc.processNetworkMessageRequest(this);
    }
}
