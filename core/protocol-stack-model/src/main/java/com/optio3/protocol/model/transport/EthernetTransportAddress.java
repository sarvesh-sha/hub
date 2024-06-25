/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.transport;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("EthernetTransportAddress")
public final class EthernetTransportAddress extends TransportAddress
{
    private static final String c_prefix = "ETHER::";

    public int d1;
    public int d2;
    public int d3;
    public int d4;
    public int d5;
    public int d6;

    @JsonCreator
    public EthernetTransportAddress(@JsonProperty("d1") int d1,
                                    @JsonProperty("d2") int d2,
                                    @JsonProperty("d3") int d3,
                                    @JsonProperty("d4") int d4,
                                    @JsonProperty("d5") int d5,
                                    @JsonProperty("d6") int d6)
    {
        this.d1 = d1;
        this.d2 = d2;
        this.d3 = d3;
        this.d4 = d4;
        this.d5 = d5;
        this.d6 = d6;
    }

    public EthernetTransportAddress(byte[] address)
    {
        Preconditions.checkNotNull(address);
        Preconditions.checkArgument(address.length == 6);

        d1 = address[0] & 0xFF;
        d2 = address[1] & 0xFF;
        d3 = address[2] & 0xFF;
        d4 = address[3] & 0xFF;
        d5 = address[4] & 0xFF;
        d6 = address[5] & 0xFF;
    }

    //--//

    @Override
    public byte[] asBytes()
    {
        byte[] res = new byte[6];

        res[0] = (byte) d1;
        res[1] = (byte) d2;
        res[2] = (byte) d3;
        res[3] = (byte) d4;
        res[4] = (byte) d5;
        res[5] = (byte) d6;

        return res;
    }

    //--//

    @Override
    public int compareTo(TransportAddress o)
    {
        return StringUtils.compareIgnoreCase(this.toString(), o.toString());
    }

    @Override
    public String toString()
    {
        return String.format("%s%02X:%02X:%02X:%02X:%02X:%02X", c_prefix, d1, d2, d3, d4, d5, d6);
    }

    public static EthernetTransportAddress fromStringNoThrow(String value)
    {
        try
        {
            byte[] buf = fromString(value);
            return new EthernetTransportAddress(buf);
        }
        catch (Throwable e)
        {
            // Any failure means bad format;
        }

        return null;
    }

    public static byte[] fromString(String value)
    {
        try
        {
            if (value.startsWith(c_prefix))
            {
                String[] parts = StringUtils.split(value.substring(c_prefix.length()), ":");
                if (parts.length == 6)
                {
                    byte[] buf = new byte[6];
                    for (int i = 0; i < parts.length; i++)
                    {
                        buf[i] = (byte) Integer.parseInt(parts[i], 16);
                    }

                    return buf;
                }
            }
        }
        catch (Throwable e)
        {
            // Any failure means bad format;
        }

        throw Exceptions.newIllegalArgumentException("Invalid MAC address: %s", value);
    }
}
