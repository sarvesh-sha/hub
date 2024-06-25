/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.DeliveryOptions;
import com.optio3.cloud.hub.model.visualization.TimeRange;
import com.optio3.cloud.hub.model.visualization.TimeRangeId;

public class ReportSchedulingOptions
{
    public TimeRangeId range;

    public ReportSchedule schedule;

    public DeliveryOptions deliveryOptions;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setFrequency(ReportFrequency frequency)
    {
        HubApplication.reportPatchCall(frequency);
        if (frequency == null)
        {
            frequency = ReportFrequency.NoRepeat;
        }

        switch (frequency)
        {
            case Daily:
            {
                range = TimeRangeId.Last24Hours;
                ReportScheduleDaily dailySchedule = new ReportScheduleDaily();
                setDefaultTime(dailySchedule);
                dailySchedule.days.addAll(Arrays.asList(DayOfWeek.values()));
                schedule = dailySchedule;
                break;
            }

            case Weekly:
            {
                range = TimeRangeId.PreviousWeek;
                ReportScheduleWeekly weeklySchedule = new ReportScheduleWeekly();
                setDefaultTime(weeklySchedule);
                weeklySchedule.dayOfWeek = DayOfWeek.SUNDAY;
                schedule                 = weeklySchedule;
                break;
            }

            case Monthly:
            {
                range = TimeRangeId.PreviousMonth;
                ReportScheduleMonthly monthlySchedule = new ReportScheduleMonthly();
                setDefaultTime(monthlySchedule);
                monthlySchedule.dayOfMonth = 1;
                schedule                   = monthlySchedule;
                break;
            }

            case NoRepeat:
            default:
            {
                range = TimeRangeId.PreviousMonth;
                ReportScheduleOnDemand onDemand = new ReportScheduleOnDemand();
                setDefaultTime(onDemand);
                schedule = onDemand;
            }
        }
    }

    public void cleanUp()
    {
        if (range == null && schedule == null)
        {
            this.setFrequency(ReportFrequency.NoRepeat);
        }
    }

    private void setDefaultTime(ReportSchedule schedule)
    {
        schedule.zoneDesired = ZoneId.systemDefault()
                                     .getId();
    }

    @JsonIgnore
    public ZonedDateTime getNextReportTime()
    {
        if (schedule == null)
        {
            return null;
        }

        return schedule.getNextReportTime();
    }

    @JsonIgnore
    public TimeRange getReportRange(ZonedDateTime reportTime)
    {
        if (schedule == null)
        {
            return null;
        }

        // Force report time to be correct zone (DB deserialization converts back to server zone)
        ZoneId desiredZone = ZoneId.of(schedule.zoneDesired);
        reportTime = reportTime.withZoneSameInstant(desiredZone);
        return range.resolve(reportTime, false);
    }
}
