/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.transport;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.lang.Unsigned32;
import com.optio3.serialization.Reflection;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("IpTransportAddress")
public final class IpTransportAddress extends TransportAddress
{
    private static final String c_prefix = "IP::";

    @JsonIgnore
    public final InetAddress address;

    @JsonCreator
    public IpTransportAddress(@JsonProperty("host") String host) throws
                                                                 UnknownHostException
    {
        this(InetAddress.getByName(host));
    }

    public IpTransportAddress(InetAddress address)
    {
        Preconditions.checkNotNull(address);

        this.address = address;
    }

    public String getHost()
    {
        return address.getHostAddress();
    }

    //--//

    @Override
    public byte[] asBytes()
    {
        try (OutputBuffer ob = new OutputBuffer())
        {
            ob.emit(address.getAddress());

            return ob.toByteArray();
        }
    }

    public TransportAddress fromBytes(byte[] buffer)
    {
        try (InputBuffer ib = InputBuffer.createFrom(buffer))
        {
            byte[] ipv4 = ib.readByteArray(4);

            return new IpTransportAddress(getAddress(ipv4));
        }
    }

    //--//

    public static InetAddress getAddress(Unsigned32 address)
    {
        int    v    = address.unbox();
        byte[] ipv4 = new byte[4];
        ipv4[0] = (byte) (v >> 24);
        ipv4[1] = (byte) (v >> 16);
        ipv4[2] = (byte) (v >> 8);
        ipv4[3] = (byte) (v);

        return getAddress(ipv4);
    }

    public static InetAddress getAddress(byte[] ipv4)
    {
        try
        {
            return InetAddress.getByAddress(ipv4);
        }
        catch (UnknownHostException e)
        {
            return null;
        }
    }

    //--//

    @Override
    public int compareTo(TransportAddress o)
    {
        IpTransportAddress other = Reflection.as(o, IpTransportAddress.class);
        if (other != null)
        {
            return compare(this, other);
        }

        return StringUtils.compareIgnoreCase(this.toString(), o.toString());
    }

    public static int compare(IpTransportAddress o1,
                              IpTransportAddress o2)
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

        return compareInetAddress(o1.address, o2.address);
    }

    public static int compareInetAddress(InetAddress o1,
                                         InetAddress o2)
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

        byte[] o1Bytes = o1.getAddress();
        byte[] o2Bytes = o2.getAddress();

        if (o1Bytes.length < o2Bytes.length)
        {
            return -1;
        }

        if (o2Bytes.length < o1Bytes.length)
        {
            return 1;
        }

        return Arrays.compareUnsigned(o1Bytes, o2Bytes);
    }

    @Override
    public String toString()
    {
        return c_prefix + getHost();
    }

    public static IpTransportAddress fromString(String value)
    {
        try
        {
            if (value.startsWith(c_prefix))
            {
                return new IpTransportAddress(value.substring(c_prefix.length()));
            }
        }
        catch (Throwable e)
        {
            // Any failure means bad format;
        }

        return null;
    }
}
