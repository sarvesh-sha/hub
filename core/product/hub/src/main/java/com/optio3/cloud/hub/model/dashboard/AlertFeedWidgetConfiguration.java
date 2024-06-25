/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.visualization.RangeSelection;

@JsonTypeName("AlertFeedWidgetConfiguration")
public class AlertFeedWidgetConfiguration extends AlertWidgetConfiguration
{
    public RangeSelection timeRange;
}