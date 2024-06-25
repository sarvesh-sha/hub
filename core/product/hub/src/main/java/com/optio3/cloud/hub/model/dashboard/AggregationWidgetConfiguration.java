/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.schedule.FilterableTimeRange;
import com.optio3.cloud.hub.model.visualization.RangeSelection;

@JsonTypeName("AggregationWidgetConfiguration")
public class AggregationWidgetConfiguration extends WidgetConfiguration
{
    public ControlPointsGroup  controlPointGroup;
    public FilterableTimeRange filterableRange;
    public boolean             hideRange;

    // TODO: UPGRADE PATCH: This setter converts it to the correct format: FilterableTimeRange
    public void setRange(RangeSelection range)
    {
        HubApplication.reportPatchCall(range);

        if (range != null)
        {
            this.filterableRange       = new FilterableTimeRange();
            this.filterableRange.range = range;
        }
    }
}
