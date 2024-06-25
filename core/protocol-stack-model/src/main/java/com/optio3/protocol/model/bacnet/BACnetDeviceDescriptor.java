/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.protocol.model.bacnet.enums.BACnetSegmentation;
import com.optio3.protocol.model.transport.TransportAddress;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("BACnetDeviceDescriptor")
public final class BACnetDeviceDescriptor extends BaseAssetDescriptor
{
    public BACnetDeviceAddress address; // The identity of the device: network/instance number.
    public BACnetAddress       bacnetAddress; // Sometimes it holds the information for the MS/TP trunk.
    public TransportAddress    transport; // The network address to reach the controller.

    public BACnetSegmentation segmentation;
    public int                maxAdpu;

    //--//

    // TODO: UPGRADE PATCH: The TransportAddress used to be passed as a JSON in text. This setter converts it to the correct format.
    public void setTransportAddress(String json)
    {
        try
        {
            transport = json != null ? ObjectMappers.SkipNulls.readValue(json, TransportAddress.class) : null;
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        BACnetDeviceDescriptor that = Reflection.as(o, BACnetDeviceDescriptor.class);
        if (that == null)
        {
            return false;
        }

        if (!Objects.equals(address, that.address))
        {
            return false;
        }

        if (transport != null && that.transport != null)
        {
            if (!transport.equals(that.transport))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(address);
    }

    @Override
    public int compareTo(BaseAssetDescriptor o)
    {
        BACnetDeviceDescriptor other = Reflection.as(o, BACnetDeviceDescriptor.class);
        if (other != null)
        {
            return compare(this, other, false);
        }

        return StringUtils.compareIgnoreCase(this.toString(), o.toString());
    }

    public static int compare(BACnetDeviceDescriptor o1,
                              BACnetDeviceDescriptor o2,
                              boolean transportFirst)
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

        int addressCompare   = o1.address.compareTo(o2.address);
        int transportCompare = compareTransportTo(o1, o2);

        if (transportFirst)
        {
            return transportCompare != 0 ? transportCompare : addressCompare;
        }
        else
        {
            return addressCompare != 0 ? addressCompare : transportCompare;
        }
    }

    private static int compareTransportTo(BACnetDeviceDescriptor o1,
                                          BACnetDeviceDescriptor o2)
    {
        if (o1.transport == o2.transport)
        {
            return 0;
        }

        if (o1.transport == null)
        {
            return 1;
        }

        if (o2.transport == null)
        {
            return -1;
        }

        return o1.transport.compareTo(o2.transport);
    }

    @Override
    public String toString()
    {
        return address.toString();
    }
}
