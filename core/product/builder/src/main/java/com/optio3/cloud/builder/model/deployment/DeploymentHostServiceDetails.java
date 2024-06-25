/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;

import com.optio3.cloud.client.hub.model.GatewayQueueStatus;

public class DeploymentHostServiceDetails
{
    public ZonedDateTime lastFetch;
    public String        remoteSysId;
    public ZonedDateTime lastUpdatedDate;
    public String        name;
    public String        url;

    public int warningThreshold;
    public int alertThreshold;

    public ZonedDateTime      lastRefresh;
    public GatewayQueueStatus queueStatus;
}
