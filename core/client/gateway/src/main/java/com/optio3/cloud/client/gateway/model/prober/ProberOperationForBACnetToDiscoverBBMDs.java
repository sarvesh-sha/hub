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

@JsonTypeName("ProberOperationForBACnetToDiscoverBBMDs")
public class ProberOperationForBACnetToDiscoverBBMDs extends ProberOperationForBACnet
{
    @JsonTypeName("ProberOperationForBACnetToDiscoverBBMDsResults") // No underscore in model name, due to Swagger issues.
    public static class Results extends BaseResults
    {
        public List<BACnetDeviceDescriptor> discoveredDevices = Lists.newArrayList();
    }

    //--//

    /**
     * The target subnet to discover, in the form {@code <IP>/<prefix>}.
     */
    public String targetSubnet;

    public int broadcastRetries = 3;
}