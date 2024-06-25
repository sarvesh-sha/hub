/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.panes.configuration;

import java.util.List;

import com.google.common.collect.Lists;

public class PaneCardConfiguration
{
    public String title;

    public List<PaneFieldConfiguration> fields = Lists.newArrayList();
}
