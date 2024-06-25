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

@JsonTypeName("ProberOperationForBACnetToDiscoverRouters")
public class ProberOperationForBACnetToDiscoverRouters extends ProberOperationForBACnet
{
    @JsonTypeName("ProberOperationForBACnetToDiscoverRoutersResults") // No underscore in model name, due to Swagger issues.
    public static class Results extends BaseResults
    {
        public List<RouterNetwork> discoveredRouters = Lists.newArrayList();
    }

    public static class RouterNetwork
    {
        public BACnetDeviceDescriptor device;
        public List<Integer>          networks = Lists.newArrayList();
    }
}