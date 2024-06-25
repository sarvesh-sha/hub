/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.linklayer;

import java.net.InetSocketAddress;

import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.protocol.model.transport.UdpTransportAddress;
import com.optio3.serialization.SerializationTag;

public final class Forwarded extends NetworkPayload
{
    @SerializationTag(number = 0)
    public Unsigned32 device_address;

    @SerializationTag(number = 1)
    public Unsigned16 device_port;

    //--//

    @Override
    public void dispatch(ServiceContext sc)
    {
        InetSocketAddress socketAddress = UdpTransportAddress.getSocketAddress(device_address, device_port);

        if (sc.owner.canReachAddress(socketAddress.getAddress()))
        {
            sc.originatingAddress = new UdpTransportAddress(socketAddress);
        }
        else
        {
            sc.debug("Ignoring originating address, not routable. Forwarded message from %s [Source: %s]", socketAddress, sc.source);
        }

        sc.debug("Received 'Forwarded' message from %s [Source: %s]", sc.originatingAddress, sc.source);

        dispatchInner(sc);
    }
}
