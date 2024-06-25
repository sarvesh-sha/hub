/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Sets;

@JsonTypeName("ReportScheduleDaily")
public class ReportScheduleDaily extends ReportSchedule
{
    public Set<DayOfWeek> days = Sets.newHashSet();

    @Override
    public ZonedDateTime getNextReportTime(ZonedDateTime nextReport)
    {
        if (days.isEmpty())
        {
            return null;
        }

        while (!days.contains(nextReport.getDayOfWeek()))
        {
            nextReport = nextReport.plusDays(1);
        }

        return nextReport;
    }
}
