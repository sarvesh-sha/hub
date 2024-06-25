/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.visualization.RangeSelection;
import com.optio3.cloud.hub.model.visualization.TimeSeriesChartConfiguration;

@JsonTypeName("TimeSeriesWidgetConfiguration")
public class TimeSeriesWidgetConfiguration extends WidgetConfiguration
{
    public RangeSelection                     range;
    public List<TimeSeriesChartConfiguration> charts;

    // TODO: UPGRADE PATCH: This setter converts it to the correct format: List<TimeSeriesChartRange>
    public void setChart(TimeSeriesChartConfiguration chart)
    {
        HubApplication.reportPatchCall(chart);

        if (chart != null)
        {
            this.charts = Lists.newArrayList();
            this.charts.add(chart);
        }
    }
}
