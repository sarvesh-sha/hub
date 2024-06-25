/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;
import java.util.List;

import com.optio3.infra.waypoint.BootConfig;

public class BootOptions
{
    public ZonedDateTime                   lastUpdated;
    public List<BootConfig.OptionAndValue> options;
}
