/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.dashboard.ControlPointsGroup;
import com.optio3.cloud.hub.model.schedule.RecurringWeeklySchedule;

@JsonTypeName("CustomReportElementAggregatedValue")
public class CustomReportElementAggregatedValue extends CustomReportElement
{
    public String             label;
    public ControlPointsGroup controlPointGroup;

    public boolean                 isFilterApplied;
    public RecurringWeeklySchedule filter;
}
