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
import com.optio3.util.TimeUtils;

public class TimeSeriesLastValueRequest
{
    public TimeSeriesPropertyRequest spec;

    //--//

    public TimeSeriesLastValueResponse fetch(SamplesCache cache)
    {
        Class<?> expectedType = cache.extractExpectedType(spec.sysId, spec.prop, false);
        if (expectedType == null)
        {
            return null;
        }

        ZonedDateTime now     = TimeUtils.now();
        Duration      maxWait = Duration.of(50, ChronoUnit.MILLIS);

        // On first pass, limit the range to last 24 hours.
        try (TimeSeriesExtract<?> extract = cache.extractSamples(spec.sysId, spec.prop, spec.convertTo, true, now.minus(1, ChronoUnit.DAYS), null, expectedType, maxWait))
        {
            if (extract.unknownProperty)
            {
                return null;
            }

            if (extract.size() > 0)
            {
                return getValue(extract);
            }
        }

        // Then to 1 week.
        try (TimeSeriesExtract<?> extract = cache.extractSamples(spec.sysId, spec.prop, spec.convertTo, true, now.minus(1, ChronoUnit.WEEKS), null, expectedType, maxWait))
        {
            if (extract.size() > 0)
            {
                return getValue(extract);
            }
        }

        // Then to 1 month.
        try (TimeSeriesExtract<?> extract = cache.extractSamples(spec.sysId, spec.prop, spec.convertTo, true, now.minus(1, ChronoUnit.MONTHS), null, expectedType, maxWait))
        {
            if (extract.size() > 0)
            {
                return getValue(extract);
            }
        }

        // If all fail, try fetching everything.
        try (TimeSeriesExtract<?> extract = cache.extractSamples(spec.sysId, spec.prop, spec.convertTo, true, null, null, expectedType, maxWait))
        {
            return getValue(extract);
        }
    }

    private TimeSeriesLastValueResponse getValue(TimeSeriesExtract<?> extract)
    {
        int pos = extract.size() - 1;
        if (pos < 0)
        {
            return null;
        }

        TimeSeriesLastValueResponse res = new TimeSeriesLastValueResponse();
        res.timestamp = TimeUtils.fromTimestampToUtcTime(extract.getNthTimestamp(pos));
        res.value     = extract.isEnum() ? extract.getValue(pos) : extract.getNthValueRaw(pos);
        return res;
    }
}
