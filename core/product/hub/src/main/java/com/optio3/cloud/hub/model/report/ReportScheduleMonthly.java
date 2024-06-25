/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("ReportScheduleMonthly")
public class ReportScheduleMonthly extends ReportSchedule
{
    public int dayOfMonth;

    @Override
    public ZonedDateTime getNextReportTime(ZonedDateTime nextReport)
    {
        if (nextReport.getDayOfMonth() > dayOfMonth)
        {
            nextReport = nextReport.plusMonths(1);
        }

        return getNthDayOfMonth(nextReport, dayOfMonth);
    }

    private static ZonedDateTime getNthDayOfMonth(ZonedDateTime time,
                                                  int dayOfMonth)
    {
        int desiredDay = Math.max(1, dayOfMonth);
        boolean isLeapYear = time.toLocalDate()
                                 .isLeapYear();
        int maxDay = time.getMonth()
                         .length(isLeapYear);

        desiredDay = Math.min(desiredDay, maxDay);

        return time.withDayOfMonth(desiredDay);
    }
}
