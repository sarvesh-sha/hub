/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway;

import com.optio3.cloud.AbstractConfiguration;

public class GatewayConfiguration extends AbstractConfiguration
{
    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setConnectionUsername(String connectionUsername)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setConnectionPassword(String connectionPassword)
    {
    }

    //--//

    public String connectionUrl;

    public String instanceId;

    //--//

    public String persistenceDirectory;
    public int    batchPeriodInSeconds;
    public int    flushToDiskDelayInSeconds;

    public int samplingPeriodForPerformanceCounters = 5 * 60; // Seconds
}
