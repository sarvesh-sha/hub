/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model.prober;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.google.common.collect.Lists;
import com.optio3.protocol.model.config.BACnetBBMD;
import com.optio3.protocol.model.config.WhoIsRange;

@JsonSubTypes({ @JsonSubTypes.Type(value = ProberOperationForBACnetToAutoDiscovery.class),
                @JsonSubTypes.Type(value = ProberOperationForBACnetToDiscoverBBMDs.class),
                @JsonSubTypes.Type(value = ProberOperationForBACnetToDiscoverDevices.class),
                @JsonSubTypes.Type(value = ProberOperationForBACnetToDiscoverRouters.class),
                @JsonSubTypes.Type(value = ProberOperationForBACnetToReadBBMDs.class),
                @JsonSubTypes.Type(value = ProberOperationForBACnetToReadDevices.class),
                @JsonSubTypes.Type(value = ProberOperationForBACnetToReadObjectNames.class),
                @JsonSubTypes.Type(value = ProberOperationForBACnetToReadObjects.class),
                @JsonSubTypes.Type(value = ProberOperationForBACnetToScanMstpTrunkForDevices.class),
                @JsonSubTypes.Type(value = ProberOperationForBACnetToScanSubnetForDevices.class) })
public abstract class ProberOperationForBACnet extends ProberOperation
{
    @JsonSubTypes({ @JsonSubTypes.Type(value = ProberOperationForBACnetToAutoDiscovery.Results.class),
                    @JsonSubTypes.Type(value = ProberOperationForBACnetToDiscoverBBMDs.Results.class),
                    @JsonSubTypes.Type(value = ProberOperationForBACnetToDiscoverDevices.Results.class),
                    @JsonSubTypes.Type(value = ProberOperationForBACnetToDiscoverRouters.Results.class),
                    @JsonSubTypes.Type(value = ProberOperationForBACnetToReadBBMDs.Results.class),
                    @JsonSubTypes.Type(value = ProberOperationForBACnetToReadDevices.Results.class),
                    @JsonSubTypes.Type(value = ProberOperationForBACnetToReadObjectNames.Results.class),
                    @JsonSubTypes.Type(value = ProberOperationForBACnetToReadObjects.Results.class),
                    @JsonSubTypes.Type(value = ProberOperationForBACnetToScanMstpTrunkForDevices.Results.class),
                    @JsonSubTypes.Type(value = ProberOperationForBACnetToScanSubnetForDevices.Results.class),
                    @JsonSubTypes.Type(value = ProberOperationForBACnetToAutoDiscovery.Results.class) })
    public static abstract class BaseResults extends ProberOperation.BaseResults
    {
    }

    //--//

    /**
     * The network configuration, in the form {@code <IP>/<prefix>}.
     */
    public String cidr;

    /**
     * If not null, the prober has to use this static IP address, instead of using DHCP.
     */
    public String staticAddress;

    /**
     * If not null, the name of the network interface to configure for this network.
     */
    public String networkInterface;

    //--//

    public boolean useUDP;
    public int     udpPort;

    public boolean useEthernet;

    public List<BACnetBBMD> bbmds = Lists.newArrayList();
    public WhoIsRange       limitScan;

    //--//

    public int defaultTimeout;

    public int maxParallelRequestsPerHost;
    public int maxParallelRequestsPerNetwork;
    public int limitPacketRate;
}
