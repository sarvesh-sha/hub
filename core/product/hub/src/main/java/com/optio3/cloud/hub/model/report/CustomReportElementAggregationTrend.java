/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.dashboard.ControlPointsGroup;
import com.optio3.cloud.hub.model.dashboard.enums.AggregationGranularity;
import com.optio3.cloud.hub.model.dashboard.enums.AggregationTrendVisualizationMode;

@JsonTypeName("CustomReportElementAggregationTrend")
public class CustomReportElementAggregationTrend extends CustomReportElement
{
    public String                            label;
    public List<ControlPointsGroup>          groups;
    public AggregationGranularity            granularity       = AggregationGranularity.Month;
    public AggregationTrendVisualizationMode visualizationMode = AggregationTrendVisualizationMode.Line;
    public boolean                           showY             = true;
    public boolean                           showLegend        = true;
}
