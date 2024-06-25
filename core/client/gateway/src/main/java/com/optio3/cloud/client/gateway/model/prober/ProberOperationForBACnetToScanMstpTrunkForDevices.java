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
import com.optio3.protocol.model.transport.TransportAddress;

@JsonTypeName("ProberOperationForBACnetToScanMstpTrunkForDevices")
public class ProberOperationForBACnetToScanMstpTrunkForDevices extends ProberOperationForBACnet
{
    @JsonTypeName("ProberOperationForBACnetToScanMstpTrunkForDevicesResults") // No underscore in model name, due to Swagger issues.
    public static class Results extends BaseResults
    {
        public List<BACnetDeviceDescriptor> discoveredDevices = Lists.newArrayList();
    }

    public static class Network
    {
        public TransportAddress transport; // The network address to reach the controller.
        public int              networkNumber;
    }

    //--//

    public List<Network> targets = Lists.newArrayList();

    public int maxRetries = 3;
}