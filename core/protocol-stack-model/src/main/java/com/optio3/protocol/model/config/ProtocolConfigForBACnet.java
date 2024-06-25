/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("ProtocolConfigForBACnet")
public class ProtocolConfigForBACnet extends ProtocolConfig
{
    public boolean useUDP = true;
    public boolean useEthernet;
    public boolean disableBroadcast;
    public boolean sweepSubnet;
    public boolean sweepMSTP;
    public boolean includeNetworksFromRouters;

    public int networkPort;

    public List<BACnetBBMD>     bbmds       = Lists.newArrayList();
    public List<FilteredSubnet> scanSubnets = Lists.newArrayList();
    public WhoIsRange           limitScan;

    public int maxParallelRequestsPerHost    = 64;
    public int maxParallelRequestsPerNetwork = 16;
    public int limitPacketRate;

    public List<NonDiscoverableBACnetDevice> nonDiscoverableDevices = Lists.newArrayList();

    public List<NonDiscoverableMstpTrunk> nonDiscoverableMstpTrunks = Lists.newArrayList();

    public List<SkippedBACnetDevice> skippedDevices = Lists.newArrayList();

    public List<FilteredSubnet> filterSubnets = Lists.newArrayList();

    public void setFlushThreshold(int val)
    {
        // Ignore legacy property.
    }

    public void setAutomaticallyConfigureSampling(boolean val)
    {
        // Ignore legacy property.
    }

    public void setReducedDiscovery(boolean val)
    {
        // Ignore legacy property.
    }

    //--//

    @Override
    public boolean equals(ProtocolConfig other)
    {
        ProtocolConfigForBACnet o = Reflection.as(other, ProtocolConfigForBACnet.class);
        if (o == null)
        {
            return false;
        }

        return equalsThroughJson(other);
    }

    // This is a fixup for a schema upgrade, from "bbmd" property to BACnetBBMD list.
    public void setBbmd(String bbmd)
    {
        if (bbmd != null)
        {
            BACnetBBMD newBBMD = new BACnetBBMD();

            String[] parts = StringUtils.split(bbmd, ':');

            newBBMD.networkAddress = parts[0];
            if (parts.length > 1)
            {
                newBBMD.networkPort = Integer.parseInt(parts[0]);
            }
            else
            {
                newBBMD.networkPort = this.networkPort > 0 ? this.networkPort : 0xBAC0;
            }

            bbmds.add(newBBMD);
        }
    }
}