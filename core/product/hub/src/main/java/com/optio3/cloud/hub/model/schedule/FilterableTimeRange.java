/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.schedule;

import com.optio3.cloud.annotation.Optio3IncludeInApiDefinitions;
import com.optio3.cloud.hub.model.visualization.RangeSelection;

@Optio3IncludeInApiDefinitions
public class FilterableTimeRange
{
    public String name;

    public RangeSelection range;

    public boolean                 isFilterApplied;
    public RecurringWeeklySchedule filter;
}
