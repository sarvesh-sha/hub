/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("TransportNetworkDescriptor")
public final class TransportNetworkDescriptor extends BaseAssetDescriptor
{
    public String transportAddress;
    public int    networkNumber;
    public String deviceIdentifier;

    //--//

    @Override
    public boolean equals(Object o)
    {
        TransportNetworkDescriptor that = Reflection.as(o, TransportNetworkDescriptor.class);
        if (that == null)
        {
            return false;
        }


        return compare(this, that) == 0;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(transportAddress, networkNumber, deviceIdentifier);
    }

    @Override
    public int compareTo(BaseAssetDescriptor o)
    {
        TransportNetworkDescriptor other = Reflection.as(o, TransportNetworkDescriptor.class);
        if (other == null)
        {
            return compare(this, other);
        }

        return StringUtils.compareIgnoreCase(this.toString(), o.toString());
    }

    public static int compare(TransportNetworkDescriptor o1,
                              TransportNetworkDescriptor o2)
    {
        if (o1 == o2)
        {
            return 0;
        }

        if (o1 == null)
        {
            return 1;
        }

        if (o2 == null)
        {
            return -1;
        }

        int diff = StringUtils.compare(o1.transportAddress, o2.transportAddress);
        if (diff == 0)
        {
            diff = Integer.compare(o1.networkNumber, o2.networkNumber);
            if (diff == 0)
            {
                diff = StringUtils.compare(o1.deviceIdentifier, o2.deviceIdentifier);
            }
        }

        return diff;
    }

    @Override
    public String toString()
    {
        if (transportAddress == null)
        {
            return "Total";
        }

        if (networkNumber < 0)
        {
            return String.format("Transport %s", transportAddress);
        }

        if (deviceIdentifier == null)
        {
            return String.format("Network %d (%s)", networkNumber, transportAddress);
        }

        return String.format("Device %d/%s (%s)", networkNumber, deviceIdentifier, transportAddress);
    }
}
