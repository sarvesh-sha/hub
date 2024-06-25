/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

package com.optio3.protocol.model.transport;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.lang.Unsigned16;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.bacnet.BACnetAddress;
import com.optio3.serialization.Reflection;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = IpTransportAddress.class), @JsonSubTypes.Type(value = UdpTransportAddress.class), @JsonSubTypes.Type(value = EthernetTransportAddress.class) })
public abstract class TransportAddress implements Comparable<TransportAddress>
{
    @Override
    public final int hashCode()
    {
        return Arrays.hashCode(asBytes());
    }

    @Override
    public final boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        TransportAddress other = Reflection.as(obj, TransportAddress.class);
        if (other != null)
        {
            return Arrays.equals(asBytes(), other.asBytes());
        }

        return false;
    }

    //--//

    public abstract byte[] asBytes();

    //--//

    public BACnetAddress create(int networkNumber)
    {
        BACnetAddress res = new BACnetAddress();
        res.network_number = Unsigned16.box(networkNumber);
        res.mac_address = asBytes();
        return res;
    }

    public static TransportAddress fromStringNoThrow(String value)
    {
        TransportAddress res = UdpTransportAddress.fromString(value);
        if (res == null)
        {
            res = IpTransportAddress.fromString(value);
            if (res == null)
            {
                res = EthernetTransportAddress.fromStringNoThrow(value);
            }
        }

        return res;
    }
}
