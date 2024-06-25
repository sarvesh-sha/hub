/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.schedule;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.model.visualization.TimeRange;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;

public class RecurringWeeklySchedule
{
    public static class Flat
    {
        public final long startSeconds;
        public final long endSeconds;

        public Flat(long startSeconds,
                    long endSeconds)
        {
            this.startSeconds = startSeconds;
            this.endSeconds   = endSeconds;
        }
    }

    public final List<DailyScheduleWithDayOfWeek> days = Lists.newArrayList();

    private Flat[] m_flattened;

    //--//

    public Flat[] flatten()
    {
        if (m_flattened == null)
        {
            List<Flat> lst = Lists.newArrayList();

            for (DailyScheduleWithDayOfWeek day : days)
            {
                if (day.dayOfWeek != null && day.dailySchedule != null && CollectionUtils.isNotEmpty(day.dailySchedule.ranges))
                {
                    int dayOffset = 86400 * (day.dayOfWeek.getValue() - 1);

                    for (RelativeTimeRange range : day.dailySchedule.ranges)
                    {
                        long start = dayOffset + range.offsetSeconds;
                        long end   = start + range.durationSeconds;

                        // Clip to one week range.
                        start = Math.max(0, Math.min(start, 86400 * 7));
                        end   = Math.max(0, Math.min(end, 86400 * 7));
                        if (start < end)
                        {
                            lst.add(new Flat(start, end));
                        }
                    }
                }
            }

            lst.sort((a, b) -> Long.compare(a.startSeconds, b.startSeconds));

            // Entries are sorted, we need to fuse overlapping ranges.
            for (int posA = 0, posB = 1; posB < lst.size(); )
            {
                Flat a = lst.get(posA);
                Flat b = lst.get(posB);

                // A's start is guaranteed not to be past B's start.
                // If B overlaps the end of A, merge the two.
                if (b.startSeconds <= a.endSeconds)
                {
                    lst.set(posA, new Flat(a.startSeconds, Math.max(a.endSeconds, b.endSeconds)));
                    lst.remove(posB);
                }
                else
                {
                    posA++;
                    posB++;
                }
            }

            m_flattened = new Flat[lst.size()];
            lst.toArray(m_flattened);
        }

        return m_flattened;
    }

    public boolean isIncluded(ZonedDateTime dateTime)
    {
        if (dateTime != null)
        {
            LocalDateTime localDateTime = dateTime.toLocalDateTime();
            LocalTime     localTime     = localDateTime.toLocalTime();
            DayOfWeek     dayOfWeek     = localDateTime.getDayOfWeek();
            int           secondOfDay   = localTime.toSecondOfDay();

            for (DailyScheduleWithDayOfWeek day : days)
            {
                if (day.dayOfWeek == dayOfWeek && day.dailySchedule != null)
                {
                    for (RelativeTimeRange range : day.dailySchedule.ranges)
                    {
                        if (range.offsetSeconds <= secondOfDay && secondOfDay < (range.offsetSeconds + range.durationSeconds))
                        {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public List<TimeRange> filter(ZonedDateTime start,
                                  ZonedDateTime end)
    {
        List<TimeRange> ranges = Lists.newArrayList();

        RecurringWeeklySchedule.Flat[] schedule = flatten();

        ZoneId    zone  = start.getZone();
        ZoneRules rules = zone.getRules();

        long timeSeconds = start.toEpochSecond();
        long endSeconds  = end.toEpochSecond();

        while (timeSeconds < endSeconds)
        {
            Instant       instant = Instant.ofEpochSecond(timeSeconds);
            ZonedDateTime date    = ZonedDateTime.ofInstant(instant, zone);

            ZoneOffsetTransition zoneOffsetTransition = rules.nextTransition(instant);
            long                 nextTransition       = zoneOffsetTransition != null ? zoneOffsetTransition.toEpochSecond() : endSeconds;

            if (nextTransition == timeSeconds)
            {
                nextTransition += 86400;
            }

            long startOfWeekSeconds = timeSeconds - getStartOfWeekOffset(date);
            long diffWeek           = timeSeconds - startOfWeekSeconds;

            long endSecondsOrTransition = Math.min(endSeconds, nextTransition);

            for (RecurringWeeklySchedule.Flat flat : schedule)
            {
                if (diffWeek < flat.startSeconds)
                {
                    // We are before the next time segment, move to the start of the time segment.
                    diffWeek = flat.startSeconds;
                }

                if (diffWeek < flat.endSeconds)
                {
                    // We are in the middle of a time segment, extract a result and move to the end of the segment.
                    long segmentStartSeconds = startOfWeekSeconds + diffWeek;
                    long segmentEndSeconds   = Math.min(startOfWeekSeconds + flat.endSeconds, endSecondsOrTransition);

                    if (segmentStartSeconds < endSecondsOrTransition)
                    {
                        ZonedDateTime segmentStart = TimeUtils.fromSecondsToUtcTime(segmentStartSeconds);
                        ZonedDateTime segmentEnd   = TimeUtils.fromSecondsToUtcTime(segmentEndSeconds);
                        ranges.add(new TimeRange(segmentStart.withZoneSameInstant(start.getZone()), segmentEnd.withZoneSameInstant(start.getZone())));
                    }

                    diffWeek = flat.endSeconds;
                }
            }

            // Move to next week.
            startOfWeekSeconds += 86400 * 7;

            // Either next week, or next transition.
            timeSeconds = Math.min(nextTransition, startOfWeekSeconds);
        }

        return ranges;
    }

    private static long getStartOfWeekOffset(ZonedDateTime date)
    {
        long dayOfWeek = date.getDayOfWeek()
                             .getValue() - 1;

        long secondOfDay = date.toLocalTime()
                               .toSecondOfDay();

        return dayOfWeek * 86400 + secondOfDay;
    }
}
