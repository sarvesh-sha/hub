/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.visualization;

import java.time.ZonedDateTime;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.model.schedule.RecurringWeeklySchedule;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesInterpolate;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;
import com.optio3.util.CollectionUtils;

public class TimeRange
{
    public ZonedDateTime start;
    public ZonedDateTime end;

    public TimeRange()
    {
    }

    public TimeRange(ZonedDateTime start,
                     ZonedDateTime end)
    {
        this.start = start;
        this.end   = end;
    }

    public List<TimeRange> filter(RecurringWeeklySchedule filter)
    {
        if (filter != null)
        {
            return filter.filter(start, end);
        }

        return Lists.newArrayList(new TimeRange(start, end));
    }

    public static double[] computeExtraTimestampsForTimeSegments(List<TimeRange> timeSegments)
    {
        return computeExtraTimestampsForTimeSegments(timeSegments, 0);
    }

    public static double[] computeExtraTimestampsForTimeSegments(List<TimeRange> timeSegments,
                                                                 double extraMargin)
    {
        if (CollectionUtils.isEmpty(timeSegments))
        {
            return null;
        }

        double[] extraTimestamps = new double[timeSegments.size() * 2];

        int pos = 0;
        for (TimeRange timeSegment : timeSegments)
        {
            extraTimestamps[pos++] = timeSegment.start.toEpochSecond() - extraMargin;
            extraTimestamps[pos++] = timeSegment.end.toEpochSecond() + extraMargin;
        }

        return extraTimestamps;
    }

    public static TimeSeriesPropertyResponse addTimestampsForTimeSegments(TimeSeriesPropertyResponse input,
                                                                          List<TimeRange> timeSegments,
                                                                          Double maxGap)
    {
        double[] extraTimestamps = computeExtraTimestampsForTimeSegments(timeSegments);

        return extraTimestamps != null ? TimeSeriesInterpolate.expandTimestamps(input, maxGap, extraTimestamps) : input;
    }
}
