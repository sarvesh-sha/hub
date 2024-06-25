/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model.prober;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.protocol.model.bacnet.BACnetDeviceDescriptor;

@JsonTypeName("ProberOperationForBACnetToDiscoverDevices")
public class ProberOperationForBACnetToDiscoverDevices extends ProberOperationForBACnet
{
    @JsonTypeName("ProberOperationForBACnetToDiscoverDevicesResults") // No underscore in model name, due to Swagger issues.
    public static class Results extends BaseResults
    {
        public List<BACnetDeviceDescriptor> discoveredDevices = Lists.newArrayList();
    }

    //--//

    public int     broadcastRetries = 3;
    public boolean sweepMSTP;
    public boolean includeNetworksFromRouters;
}