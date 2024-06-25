/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.panes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.dashboard.ControlPointsGroup;

@JsonTypeName("PaneFieldAggregatedValue")
public class PaneFieldAggregatedValue extends PaneField
{
    public ControlPointsGroup value;
}
