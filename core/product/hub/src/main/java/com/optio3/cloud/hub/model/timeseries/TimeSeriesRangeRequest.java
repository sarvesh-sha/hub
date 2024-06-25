/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import com.optio3.cloud.hub.logic.samples.SamplesCache;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;

public class TimeSeriesRangeRequest
{
    public TimeSeriesPropertyRequest spec;
    public ZonedDateTime             rangeStart;
    public ZonedDateTime             rangeEnd;

    //--//

    public TimeSeriesRangeResponse fetch(SamplesCache cache)
    {
        TimeSeries.NumericValueRanges values = cache.collectSampleRanges(spec.sysId, spec.prop, spec.convertTo, rangeStart, rangeEnd, Duration.of(100, ChronoUnit.MILLIS));
        if (values == null)
        {
            return null;
        }

        TimeSeriesRangeResponse result = new TimeSeriesRangeResponse();
        result.firstTimestamp         = values.firstTimestamp;
        result.lastTimestamp          = values.lastTimestamp;
        result.numberOfMissingSamples = values.numberOfMissingSamples;
        result.numberOfSamples        = values.numberOfSamples;

        boolean firstRange = true;
        long    timeSum    = 0;
        double  valueSum   = 0;

        for (TimeSeries.NumericValueRange range : values.ranges)
        {
            double minValue = Math.min(range.valueLeft, range.valueRight);
            double maxValue = Math.max(range.valueLeft, range.valueRight);

            if (firstRange)
            {
                firstRange          = false;
                result.minValue     = minValue;
                result.maxValue     = maxValue;
                result.averageValue = (range.valueLeft + range.valueRight) / 2;
            }
            else
            {
                result.minValue = Math.min(result.minValue, minValue);
                result.maxValue = Math.max(result.maxValue, maxValue);
            }

            //
            // Since samples don't always come in at a regular interval,
            // we need to integrate the area under the curve,
            // approximated by a trapezoid.
            //
            timeSum += range.delta;
            valueSum += range.delta * (range.valueLeft + range.valueRight) / 2;
        }

        if (timeSum > 0)
        {
            result.averageValue = valueSum / timeSum;
        }

        return result;
    }
}
