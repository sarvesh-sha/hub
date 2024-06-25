/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.dashboard.enums.AggregationGranularity;
import com.optio3.cloud.hub.model.dashboard.enums.AggregationTrendVisualizationMode;
import com.optio3.cloud.hub.model.schedule.FilterableTimeRange;

@JsonTypeName("AggregationTrendWidgetConfiguration")
public class AggregationTrendWidgetConfiguration extends WidgetConfiguration
{
    public List<ControlPointsGroup>          groups;
    public FilterableTimeRange               filterableRange;
    public AggregationGranularity            granularity       = AggregationGranularity.Month;
    public AggregationTrendVisualizationMode visualizationMode = AggregationTrendVisualizationMode.Line;
    public boolean                           showY             = true;
    public boolean                           showLegend        = true;
}
