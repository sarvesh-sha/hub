/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.transport;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned32;
import com.optio3.serialization.Reflection;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("UdpTransportAddress")
public final class UdpTransportAddress extends TransportAddress
{
    private static final String c_prefix = "UDP::";

    @JsonIgnore
    public final InetSocketAddress socketAddress;

    @JsonCreator
    public UdpTransportAddress(@JsonProperty("host") String host,
                               @JsonProperty("port") int port) throws
                                                               UnknownHostException
    {
        this(new InetSocketAddress(InetAddress.getByName(host), port));
    }

    public UdpTransportAddress(InetSocketAddress address)
    {
        Preconditions.checkNotNull(address);

        socketAddress = address;
    }

    public String getHost()
    {
        return socketAddress.getHostString();
    }

    public int getPort()
    {
        return socketAddress.getPort();
    }

    //--//

    @Override
    public byte[] asBytes()
    {
        try (OutputBuffer ob = new OutputBuffer())
        {
            InetAddress addr = socketAddress.getAddress();
            ob.emit(addr.getAddress());
            ob.emit2Bytes(socketAddress.getPort());

            return ob.toByteArray();
        }
    }

    public TransportAddress fromBytes(byte[] buffer)
    {
        try (InputBuffer ib = InputBuffer.createFrom(buffer))
        {
            byte[] ipv4 = ib.readByteArray(4);
            int    port = ib.read2BytesUnsigned();

            return new UdpTransportAddress(getSocketAddress(ipv4, port));
        }
    }

    //--//

    public static InetSocketAddress getSocketAddress(Unsigned32 address,
                                                     Unsigned16 port)
    {
        int    v    = address.unbox();
        byte[] ipv4 = new byte[4];
        ipv4[0] = (byte) (v >> 24);
        ipv4[1] = (byte) (v >> 16);
        ipv4[2] = (byte) (v >> 8);
        ipv4[3] = (byte) (v);

        return getSocketAddress(ipv4, port.unboxUnsigned());
    }

    public static InetSocketAddress getSocketAddress(byte[] ipv4,
                                                     int port)
    {
        try
        {
            return new InetSocketAddress(InetAddress.getByAddress(ipv4), port);
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
        UdpTransportAddress other = Reflection.as(o, UdpTransportAddress.class);
        if (other != null)
        {
            return compare(this, other);
        }

        return StringUtils.compareIgnoreCase(this.toString(), o.toString());
    }

    public static int compare(UdpTransportAddress o1,
                              UdpTransportAddress o2)
    {
        if (o1 == o2)
        {
            return 0;
        }

        if (o1 == null || o1.socketAddress == null)
        {
            return 1;
        }

        if (o2 == null || o2.socketAddress == null)
        {
            return -1;
        }

        int diff = IpTransportAddress.compareInetAddress(o1.socketAddress.getAddress(), o2.socketAddress.getAddress());
        if (diff == 0)
        {
            diff = o1.socketAddress.getPort() - o2.socketAddress.getPort();
        }

        return diff;
    }

    @Override
    public String toString()
    {
        return c_prefix + getHost() + ":" + getPort();
    }

    public static UdpTransportAddress fromString(String value)
    {
        try
        {
            if (value.startsWith(c_prefix))
            {
                String[] parts = StringUtils.split(value.substring(c_prefix.length()), ":");
                if (parts.length == 2)
                {
                    return new UdpTransportAddress(parts[0], Integer.parseInt(parts[1]));
                }
            }
        }
        catch (Throwable e)
        {
            // Any failure means bad format;
        }

        return null;
    }
}
