/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.google.common.collect.Sets;
import com.optio3.cloud.hub.model.report.ReportScheduleDaily;
import com.optio3.cloud.hub.model.report.ReportScheduleMonthly;
import com.optio3.cloud.hub.model.report.ReportScheduleWeekly;
import org.junit.Test;

public class ReportsTest
{
    @Test
    public void testDailySchedule()
    {
        ZoneId mountainZone = ZoneId.of("US/Mountain");
        ZoneId easternZone  = ZoneId.of("US/Eastern");

        ReportScheduleDaily schedule = new ReportScheduleDaily();
        schedule.days        = Sets.newHashSet(DayOfWeek.values());
        schedule.hour        = 14;
        schedule.minute      = 0;
        schedule.zoneDesired = mountainZone.getId();
        ZonedDateTime timeOfDay = ZonedDateTime.of(2020, 10, 30, 14, 0, 0, 0, easternZone);

        ZonedDateTime next = schedule.getNextReportTime();
        assertEquals(14, next.getHour());
        assertEquals(mountainZone, next.getZone());

        ZonedDateTime start = timeOfDay.withZoneSameLocal(mountainZone);

        next = schedule.getNextReportTime(start);
        assertEquals(start, next);

        schedule.days.remove(DayOfWeek.FRIDAY);
        next = schedule.getNextReportTime(start);
        assertEquals(start.plusDays(1), next);

        // Crossing DST boundary
        schedule.days.remove(DayOfWeek.SATURDAY);
        next = schedule.getNextReportTime(start);
        assertEquals(start.plusDays(2), next);
        assertEquals(14, next.getHour());
    }

    @Test
    public void testWeeklySchedule()
    {
        ZoneId mountainZone = ZoneId.of("US/Mountain");
        ZoneId easternZone  = ZoneId.of("US/Eastern");

        ReportScheduleWeekly schedule = new ReportScheduleWeekly();
        schedule.dayOfWeek   = DayOfWeek.FRIDAY;
        schedule.hour        = 14;
        schedule.minute      = 0;
        schedule.zoneDesired = mountainZone.getId();
        ZonedDateTime timeOfDay = ZonedDateTime.of(2020, 10, 30, 14, 0, 0, 0, easternZone);

        ZonedDateTime next = schedule.getNextReportTime();
        assertEquals(14, next.getHour());
        assertEquals(mountainZone, next.getZone());
        assertEquals(DayOfWeek.FRIDAY, next.getDayOfWeek());

        ZonedDateTime start = timeOfDay.withZoneSameLocal(mountainZone);

        next = schedule.getNextReportTime(start);
        assertEquals(start, next);

        // Crossing DST boundary
        schedule.dayOfWeek = DayOfWeek.SUNDAY;
        next               = schedule.getNextReportTime(start);
        assertEquals(start.plusDays(2), next);
        assertEquals(14, next.getHour());
    }

    @Test
    public void testMonthlySchedule()
    {
        ZoneId mountainZone = ZoneId.of("US/Mountain");
        ZoneId easternZone  = ZoneId.of("US/Eastern");

        ReportScheduleMonthly schedule = new ReportScheduleMonthly();
        schedule.dayOfMonth  = 1;
        schedule.hour        = 14;
        schedule.minute      = 0;
        schedule.zoneDesired = mountainZone.getId();
        ZonedDateTime timeOfDay = ZonedDateTime.of(2020, 10, 30, 14, 0, 0, 0, easternZone);

        ZonedDateTime next = schedule.getNextReportTime();
        assertEquals(14, next.getHour());
        assertEquals(mountainZone, next.getZone());
        assertEquals(1, next.getDayOfMonth());

        ZonedDateTime start = timeOfDay.withZoneSameLocal(mountainZone);

        schedule.dayOfMonth = 30;
        next                = schedule.getNextReportTime(start);
        assertEquals(start, next);

        // Crossing DST boundary
        schedule.dayOfMonth = 1;
        next                = schedule.getNextReportTime(start);
        assertEquals(start.plusDays(2), next);
        assertEquals(14, next.getHour());

        // Ensure we don't go over max days, Feb in a leap year
        schedule.dayOfMonth = 31;
        next                = schedule.getNextReportTime(start.withMonth(2));
        assertEquals(start.withMonth(2)
                          .withDayOfMonth(29), next);
        assertEquals(14, next.getHour());
    }
}
