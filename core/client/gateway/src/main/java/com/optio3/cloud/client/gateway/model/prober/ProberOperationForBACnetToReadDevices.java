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

@JsonTypeName("ProberOperationForBACnetToReadDevices")
public class ProberOperationForBACnetToReadDevices extends ProberOperationForBACnet
{
    @JsonTypeName("ProberOperationForBACnetToReadDevicesResults") // No underscore in model name, due to Swagger issues.
    public static class Results extends BaseResults
    {
        public List<ProberObjectBACnet> objects = Lists.newArrayList();
    }

    //--//

    public List<BACnetDeviceDescriptor> targetDevices = Lists.newArrayList();
}