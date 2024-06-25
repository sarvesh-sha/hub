/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;

public class JsonWebSocketDnsHints extends HashMap<String, List<String>>
{
    public List<InetAddress> lookup(String host)
    {
        try
        {
            List<String> ipAddresses = get(host);
            if (ipAddresses != null)
            {
                List<InetAddress> res = Lists.newArrayList();

                for (String ip : ipAddresses)
                {
                    res.add(InetAddress.getByName(ip));
                }

                return res;
            }
        }
        catch (Throwable t)
        {
            // Ignore failures.
        }

        return null;
    }

    public static String prepareForYAML(String host) throws
                                                     UnknownHostException
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("  %s:\n", host));

        for (InetAddress inetAddress : InetAddress.getAllByName(host))
        {
            sb.append(String.format("    - %s\n", inetAddress.getHostAddress()));
        }

        return sb.toString();
    }
}
