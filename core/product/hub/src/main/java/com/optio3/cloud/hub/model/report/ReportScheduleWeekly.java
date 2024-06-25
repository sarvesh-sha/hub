/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("ReportScheduleWeekly")
public class ReportScheduleWeekly extends ReportSchedule
{
    public DayOfWeek dayOfWeek;

    @Override
    public ZonedDateTime getNextReportTime(ZonedDateTime nextReport)
    {
        if (dayOfWeek == null)
        {
            return null;
        }

        while (nextReport.getDayOfWeek() != dayOfWeek)
        {
            nextReport = nextReport.plusDays(1);
        }

        return nextReport;
    }
}
