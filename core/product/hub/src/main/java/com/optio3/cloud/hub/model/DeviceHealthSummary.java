/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.model.alert.AlertSeverity;

public class DeviceHealthSummary
{
    public AlertSeverity overallStatus;

    public List<DeviceHealthAggregate> countsByType = Lists.newArrayList();
}
