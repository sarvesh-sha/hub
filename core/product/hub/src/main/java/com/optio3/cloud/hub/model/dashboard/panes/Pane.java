/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.panes;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;
import com.optio3.cloud.hub.model.dashboard.BrandingConfiguration;

@Optio3IncludeInApiDefinitions
public class Pane
{
    public String title;

    public BrandingConfiguration branding;

    public List<PaneCard> cards = Lists.newArrayList();
}
