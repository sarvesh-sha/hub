/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueTimeZone;
import com.optio3.cloud.hub.engine.core.value.EngineValueWeeklySchedule;
import com.optio3.cloud.hub.engine.metrics.MetricsEngineExecutionContext;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesInterpolate;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;
import com.optio3.cloud.hub.model.visualization.TimeRange;

@JsonSubTypes({ @JsonSubTypes.Type(value = MetricsEngineOperatorFilterInsideSchedule.class), @JsonSubTypes.Type(value = MetricsEngineOperatorFilterOutsideSchedule.class) })
public abstract class MetricsEngineOperatorBaseWithSchedule extends EngineOperatorBinaryFromMetrics<MetricsEngineValueSeries, MetricsEngineValueSeries, EngineValueWeeklySchedule>
{
    public MetricsEngineOperatorBaseWithSchedule()
    {
        super(MetricsEngineValueSeries.class);
    }

    protected MetricsEngineValueSeries computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     MetricsEngineValueSeries series,
                                                     EngineValueWeeklySchedule schedule,
                                                     boolean keepInside)
    {
        stack.checkNonNullValue(series, "Missing left parameter");
        stack.checkNonNullValue(schedule, "Missing right parameter");

        MetricsEngineExecutionContext ctx2 = (MetricsEngineExecutionContext) ctx;

        //
        // Give precedence to the explicit timezone in the schedule.
        //
        ZoneId zoneId = EngineValueTimeZone.resolve(stack, schedule.zoneCreated);
        if (zoneId == null)
        {
            zoneId = series.values.timeZone;
            if (zoneId == null)
            {
                zoneId = EngineValueWeeklySchedule.c_defaultZone;
            }
        }

        ZonedDateTime rangeStart = ctx2.rangeStart;
        ZonedDateTime rangeEnd   = ctx2.rangeEnd;

        rangeStart = rangeStart.withZoneSameInstant(zoneId);
        rangeEnd   = rangeEnd.withZoneSameInstant(zoneId);

        TimeRange       tr           = new TimeRange(rangeStart, rangeEnd);
        List<TimeRange> timeSegments = tr.filter(schedule.value);

        double[] extraTimestamps  = TimeRange.computeExtraTimestampsForTimeSegments(timeSegments);
        double   replacementValue = series.values.shouldInterpolate() ? Double.NaN : 0.0;

        if (extraTimestamps == null)
        {
            MetricsEngineValueSeries res = series.copy();

            if (keepInside)
            {
                Arrays.fill(res.values.values, replacementValue);
            }

            return res;
        }
        else
        {
            double[] extraTimestampsMargin = TimeRange.computeExtraTimestampsForTimeSegments(timeSegments, keepInside ? 0.002 : -0.002);

            TimeSeriesPropertyResponse outputValues = TimeSeriesInterpolate.expandTimestamps(series.values, ctx2.maxInterpolationGap, extraTimestamps, extraTimestampsMargin);

            double[] timestamps = outputValues.timestamps;
            double[] values     = outputValues.values;

            double nextSegmentStart  = extraTimestamps[0];
            double nextSegmentEnd    = extraTimestamps[1];
            int    nextSegmentCursor = 2;
            int    cursorOut         = 0;

            while (cursorOut < values.length)
            {
                //
                // Zero all the values before the next segment.
                //
                while (cursorOut < values.length && timestamps[cursorOut] < nextSegmentStart)
                {
                    if (keepInside)
                    {
                        values[cursorOut] = replacementValue;
                    }

                    cursorOut++;
                }

                //
                // Always include samples on the boundary.
                //
                while (cursorOut < values.length && timestamps[cursorOut] == nextSegmentStart)
                {
                    cursorOut++;
                }

                //
                // Skip over the range.
                //
                while (cursorOut < values.length && timestamps[cursorOut] < nextSegmentEnd)
                {
                    if (!keepInside)
                    {
                        values[cursorOut] = replacementValue;
                    }

                    cursorOut++;
                }

                //
                // Always include samples on the boundary.
                //
                while (cursorOut < values.length && timestamps[cursorOut] == nextSegmentEnd)
                {
                    cursorOut++;
                }

                //
                // Move to the next segment or assume a segment at infinity.
                //
                if (nextSegmentCursor < extraTimestamps.length)
                {
                    nextSegmentStart = extraTimestamps[nextSegmentCursor];
                    nextSegmentEnd   = extraTimestamps[nextSegmentCursor + 1];

                    nextSegmentCursor += 2;
                }
                else
                {
                    nextSegmentStart = Double.MAX_VALUE;
                    nextSegmentEnd   = Double.MAX_VALUE;
                }
            }

            return new MetricsEngineValueSeries(outputValues);
        }
    }
}
