/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.protocol.model.config.ProtocolConfig;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public final class GatewayNetwork
{
    public final static class Delta
    {
        public final List<GatewayNetwork> added   = Lists.newArrayList();
        public final List<GatewayNetwork> same    = Lists.newArrayList();
        public final List<GatewayNetwork> changed = Lists.newArrayList();
        public final List<GatewayNetwork> removed = Lists.newArrayList();

        public Delta(List<GatewayNetwork> listBefore,
                     List<GatewayNetwork> listAfter)
        {
            Map<String, GatewayNetwork> lookupBefore = Maps.newHashMap();
            Map<String, GatewayNetwork> lookupAfter  = Maps.newHashMap();

            updateLookup(lookupBefore, listBefore);
            updateLookup(lookupAfter, listAfter);

            for (GatewayNetwork gnAfter : listAfter)
            {
                GatewayNetwork gnBefore = lookupBefore.get(gnAfter.sysId);
                if (gnBefore == null)
                {
                    added.add(gnAfter);
                }
                else if (gnBefore.equals(gnAfter))
                {
                    same.add(gnAfter);
                }
                else
                {
                    changed.add(gnAfter);
                }
            }

            for (GatewayNetwork gnBefore : listBefore)
            {
                if (!lookupAfter.containsKey(gnBefore.sysId))
                {
                    removed.add(gnBefore);
                }
            }
        }

        public boolean hasChanged()
        {
            return added.size() > 0 || changed.size() > 0 || removed.size() > 0;
        }

        private void updateLookup(Map<String, GatewayNetwork> lookup,
                                  List<GatewayNetwork> list)
        {
            for (GatewayNetwork gn : list)
            {
                lookup.put(gn.sysId, gn);
            }
        }
    }

    /**
     * The DB identifier for this network.
     */
    public String sysId;

    /**
     * The name for this network.
     */
    public String name;
    public String namePadded;

    /**
     * The network configuration, in the form {@code <IP>/<prefix>}.
     */
    public String cidr;

    /**
     * If not null, the Gateway has to use this static IP address, instead of using DHCP.
     */
    public String staticAddress;

    /**
     * If not null, the name of the network interface to configure for this network.
     */
    public String networkInterface;

    /**
     * Protocol-specific configuration, i.e. ports to scan, password to use, etc.
     */
    public final List<ProtocolConfig> protocolsConfiguration = Lists.newArrayList();

    //--//

    @Override
    public int hashCode()
    {
        return cidr != null ? cidr.hashCode() : 0;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        GatewayNetwork that = Reflection.as(o, GatewayNetwork.class);
        if (that == null)
        {
            return false;
        }

        if (!StringUtils.equals(cidr, that.cidr))
        {
            return false;
        }

        if (!StringUtils.equals(staticAddress, that.staticAddress))
        {
            return false;
        }

        if (!StringUtils.equals(networkInterface, that.networkInterface))
        {
            return false;
        }

        if (!CollectionUtils.equals(protocolsConfiguration, that.protocolsConfiguration, ProtocolConfig::equals))
        {
            return false;
        }

        return true;
    }

    //--//

    public <T extends ProtocolConfig> boolean hasProtocolConfiguration(Class<T> clz)
    {
        return getProtocolConfiguration(clz) != null;
    }

    public <T extends ProtocolConfig> T getProtocolConfiguration(Class<T> clz)
    {
        ProtocolConfig res = CollectionUtils.findFirst(protocolsConfiguration, clz::isInstance);
        return clz.cast(res);
    }
}
