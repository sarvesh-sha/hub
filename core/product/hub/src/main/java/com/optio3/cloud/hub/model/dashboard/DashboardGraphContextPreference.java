/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;

@Optio3IncludeInApiDefinitions
public class DashboardGraphContextPreference
{
    public final Map<String, Map<String, String>> graphContexts = Maps.newHashMap();
}
