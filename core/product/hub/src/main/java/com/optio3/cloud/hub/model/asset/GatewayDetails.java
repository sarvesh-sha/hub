/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset;

import java.time.ZonedDateTime;
import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.cloud.client.gateway.model.GatewayQueueStatus;

public class GatewayDetails
{
    public ZonedDateTime lastRefresh;

    public int availableProcessors;

    public long freeMemory;

    public long totalMemory;

    public long maxMemory;

    public int hardwareVersion;
    public int firmwareVersion;

    public Map<String, String> networkInterfaces = Maps.newHashMap();

    public GatewayQueueStatus queueStatus;
}
