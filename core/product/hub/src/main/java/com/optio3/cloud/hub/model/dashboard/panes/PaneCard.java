/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.panes;

import java.util.List;

import com.google.common.collect.Lists;

public class PaneCard
{
    public String title;

    public List<PaneField> fields = Lists.newArrayList();
}
