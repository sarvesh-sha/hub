/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.optio3.cloud.hub.model.schedule.DailySchedule;
import com.optio3.cloud.hub.model.schedule.DailyScheduleWithDayOfWeek;
import com.optio3.cloud.hub.model.schedule.RecurringWeeklySchedule;
import com.optio3.cloud.hub.model.schedule.RelativeTimeRange;
import com.optio3.cloud.hub.model.visualization.TimeRange;
import org.junit.Test;

public class WeeklyScheduleTest
{
    @Test
    public void testWinterToSummer()
    {
        ZoneId zoneId = ZoneId.of("America/Los_Angeles");

        // On the 8th of March, DST kicks in.
        TimeRange tr = new TimeRange();
        tr.start = ZonedDateTime.of(2020, 3, 5, 0, 0, 0, 0, zoneId);
        tr.end   = ZonedDateTime.of(2020, 3, 10, 0, 0, 0, 0, zoneId);

        RecurringWeeklySchedule schedule = new RecurringWeeklySchedule();

        for (DayOfWeek dayOfWeek : DayOfWeek.values())
        {
            RelativeTimeRange relativeRange = new RelativeTimeRange();
            relativeRange.offsetSeconds   = 1 * 3600;
            relativeRange.durationSeconds = 3 * 3600;

            DailyScheduleWithDayOfWeek day = new DailyScheduleWithDayOfWeek();
            day.dayOfWeek     = dayOfWeek;
            day.dailySchedule = new DailySchedule();
            day.dailySchedule.ranges.add(relativeRange);

            schedule.days.add(day);
        }

        List<TimeRange> ranges = tr.filter(schedule);
        assertEquals(6, ranges.size());
        assertEquals(3 * 3600, durationInSeconds(ranges.get(0)));
        assertEquals(3 * 3600, durationInSeconds(ranges.get(1)));
        assertEquals(3 * 3600, durationInSeconds(ranges.get(2)));
        assertEquals(1 * 3600, durationInSeconds(ranges.get(3)));
        assertEquals(1 * 3600, durationInSeconds(ranges.get(4)));
        assertEquals(3 * 3600, durationInSeconds(ranges.get(5)));
    }

    @Test
    public void testWinterToSummerNonExistingTime()
    {
        ZoneId zoneId = ZoneId.of("America/Los_Angeles");

        // On the 8th of March, DST kicks in.
        TimeRange tr = new TimeRange();
        tr.start = ZonedDateTime.of(2020, 3, 5, 0, 0, 0, 0, zoneId);
        tr.end   = ZonedDateTime.of(2020, 3, 10, 0, 0, 0, 0, zoneId);

        RecurringWeeklySchedule schedule = new RecurringWeeklySchedule();

        RelativeTimeRange relativeRange = new RelativeTimeRange();
        relativeRange.offsetSeconds   = 2 * 3600;
        relativeRange.durationSeconds = 3600;

        DailyScheduleWithDayOfWeek day = new DailyScheduleWithDayOfWeek();
        day.dayOfWeek     = DayOfWeek.SUNDAY;
        day.dailySchedule = new DailySchedule();
        day.dailySchedule.ranges.add(relativeRange);

        schedule.days.add(day);

        List<TimeRange> ranges = tr.filter(schedule);
        assertEquals(0, ranges.size());
    }

    @Test
    public void testSummerToWinter()
    {
        ZoneId zoneId = ZoneId.of("America/Los_Angeles");

        // On the first of November, DST kicks out.
        TimeRange tr = new TimeRange();
        tr.start = ZonedDateTime.of(2020, 10, 30, 0, 0, 0, 0, zoneId);
        tr.end   = ZonedDateTime.of(2020, 11, 4, 0, 0, 0, 0, zoneId);

        RecurringWeeklySchedule schedule = new RecurringWeeklySchedule();

        for (DayOfWeek dayOfWeek : DayOfWeek.values())
        {
            RelativeTimeRange relativeRange = new RelativeTimeRange();
            relativeRange.offsetSeconds   = 1 * 3600;
            relativeRange.durationSeconds = 3 * 3600;

            DailyScheduleWithDayOfWeek day = new DailyScheduleWithDayOfWeek();
            day.dayOfWeek     = dayOfWeek;
            day.dailySchedule = new DailySchedule();
            day.dailySchedule.ranges.add(relativeRange);

            schedule.days.add(day);
        }

        List<TimeRange> ranges = tr.filter(schedule);
        assertEquals(6, ranges.size());
        assertEquals(3 * 3600, durationInSeconds(ranges.get(0)));
        assertEquals(3 * 3600, durationInSeconds(ranges.get(1)));
        assertEquals(1 * 3600, durationInSeconds(ranges.get(2)));
        assertEquals(3 * 3600, durationInSeconds(ranges.get(3)));
        assertEquals(3 * 3600, durationInSeconds(ranges.get(4)));
        assertEquals(3 * 3600, durationInSeconds(ranges.get(5)));
    }

    @Test
    public void testInclusion()
    {
        ZoneId zoneId = ZoneId.of("America/Los_Angeles");

        RecurringWeeklySchedule schedule = new RecurringWeeklySchedule();

        RelativeTimeRange relativeRange = new RelativeTimeRange();
        relativeRange.offsetSeconds   = 1 * 3600;
        relativeRange.durationSeconds = 3 * 3600;

        DailyScheduleWithDayOfWeek day = new DailyScheduleWithDayOfWeek();
        day.dayOfWeek     = DayOfWeek.MONDAY;
        day.dailySchedule = new DailySchedule();
        day.dailySchedule.ranges.add(relativeRange);

        schedule.days.add(day);

        // Wednesday
        assertFalse(schedule.isIncluded(ZonedDateTime.of(2020, 11, 4, 0, 59, 59, 0, zoneId)));
        assertFalse(schedule.isIncluded(ZonedDateTime.of(2020, 11, 4, 1, 0, 0, 0, zoneId)));
        assertFalse(schedule.isIncluded(ZonedDateTime.of(2020, 11, 4, 3, 59, 59, 0, zoneId)));

        // Monday
        assertFalse(schedule.isIncluded(ZonedDateTime.of(2020, 11, 2, 0, 59, 59, 0, zoneId)));
        assertTrue(schedule.isIncluded(ZonedDateTime.of(2020, 11, 2, 1, 0, 0, 0, zoneId)));
        assertTrue(schedule.isIncluded(ZonedDateTime.of(2020, 11, 2, 3, 59, 59, 0, zoneId)));
        assertFalse(schedule.isIncluded(ZonedDateTime.of(2020, 11, 2, 4, 0, 0, 0, zoneId)));
    }

    private static long durationInSeconds(TimeRange range)
    {
        return Duration.between(range.start, range.end)
                       .toSeconds();
    }
}
