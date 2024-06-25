/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet;

import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.optio3.lang.Unsigned16;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationTag;
import com.optio3.util.Base64EncodedValue;
import com.optio3.util.BufferUtils;
import org.apache.commons.lang3.StringUtils;

public final class BACnetAddress extends Sequence
{
    public static final BACnetAddress GlobalBroadcast;

    public static final int LocalNetwork  = 0; // A value of 0 indicates the local network
    public static final int GlobalNetwork = 0xFF_FF;

    static
    {
        GlobalBroadcast                = new BACnetAddress();
        GlobalBroadcast.network_number = Unsigned16.box(GlobalNetwork);
    }

    @SerializationTag(number = 0)
    @BACnetSerializationTag(untagged = true)
    public Unsigned16 network_number; // A value of 0 indicates the local network

    @SerializationTag(number = 1)
    @BACnetSerializationTag(untagged = true)
    public byte[] mac_address; // A string of length 0 indicates a broadcast

    @JsonGetter("mac_address")
    public String getMacAddress()
    {
        return printHex(mac_address);
    }

    @JsonSetter("mac_address")
    public void setMacAddress(String value)
    {
        if (StringUtils.isNotEmpty(value))
        {
            byte[] buf = parseHex(value);
            if (buf == null)
            {
                //
                // We used to serialize "mac_address" as a byte[], which Jackson converts into Base64.
                // For legacy reasons, let's try decoding it as such.
                //
                Base64EncodedValue encodedValue = new Base64EncodedValue(value);
                buf = encodedValue.getValue();
            }

            this.mac_address = buf;
        }
        else
        {
            this.mac_address = null;
        }
    }

    //--//

    public static BACnetAddress createMstp(int network,
                                           int instance)
    {
        BACnetAddress res = new BACnetAddress();
        res.network_number = Unsigned16.box(network);
        res.mac_address    = new byte[1];
        res.mac_address[0] = (byte) instance;

        return res;
    }

    public boolean couldBeMstp()
    {
        return mac_address != null && mac_address.length == 1;
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        BACnetAddress that = Reflection.as(o, BACnetAddress.class);
        if (that == null)
        {
            return false;
        }

        return Objects.equals(network_number, that.network_number) && Arrays.equals(mac_address, that.mac_address);
    }

    @Override
    public int hashCode()
    {
        int result = 1;

        result = 31 * result + Objects.hashCode(network_number);
        result = 31 * result + Arrays.hashCode(mac_address);

        return result;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        int networkNumber = Unsigned16.unboxUnsignedOrDefault(network_number, (short) LocalNetwork);
        if (networkNumber == LocalNetwork)
        {
            sb.append("LOCAL");
        }
        else if (networkNumber == GlobalNetwork)
        {
            sb.append("GLOBAL");
        }
        else
        {
            sb.append(String.format("%d", networkNumber));
        }

        String mac = printHex(mac_address);
        if (mac != null)
        {
            sb.append("/");
            sb.append(mac);
        }

        return sb.toString();
    }

    public static BACnetAddress fromString(String value)
    {
        try
        {
            BACnetAddress res = new BACnetAddress();

            String[] parts = StringUtils.split(value, '/');
            String   left  = parts[0];

            if ("LOCAL".equals(left))
            {
                res.network_number = Unsigned16.box(LocalNetwork);
            }
            else if ("GLOBAL".equals(left))
            {
                res.network_number = Unsigned16.box(GlobalNetwork);
            }
            else
            {
                res.network_number = Unsigned16.box(Integer.parseInt(left));
            }

            if (parts.length == 2)
            {
                res.mac_address = parseHex(parts[1]);
            }

            return res;
        }
        catch (Throwable e)
        {
            // Any failure means bad format;
        }

        return null;
    }

    //--//

    private static byte[] parseHex(String val)
    {
        int size = val.length() + 1;

        if (size % 3 != 0)
        {
            return null;
        }

        int    numDigits = size / 3;
        byte[] res       = new byte[numDigits];

        for (int i = 0; i < numDigits; i++)
        {
            if (i > 0 && val.charAt(i * 3 - 1) != ':')
            {
                return null;
            }

            int cHigh = BufferUtils.fromHex(val.charAt(i * 3 + 0));
            int cLow  = BufferUtils.fromHex(val.charAt(i * 3 + 1));

            if (cHigh < 0 || cLow < 0)
            {
                return null;
            }

            res[i] = (byte) (cHigh * 16 + cLow);
        }

        return res;
    }

    private static String printHex(byte[] val)
    {
        if (val == null || val.length == 0)
        {
            return null;
        }

        StringBuilder sb = new StringBuilder(val.length * 3);

        for (int i = 0; i < val.length; i++)
        {
            if (i != 0)
            {
                sb.append(":");
            }

            int c = val[i];
            sb.append(BufferUtils.toHex(c >> 4));
            sb.append(BufferUtils.toHex(c >> 0));
        }

        return sb.toString();
    }
}
