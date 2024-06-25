/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.optio3.cloud.hub.logic.samples.SamplesCache;

public class TimeSeriesSinglePropertyRequest extends TimeSeriesBaseRequest
{
    public TimeSeriesPropertyRequest spec;

    public boolean deltaEncode;

    //--//

    public TimeSeriesSinglePropertyResponse fetch(SamplesCache cache)
    {
        TimeSeriesPropertyResponse specResults = spec.fetch(cache, this, Duration.of(100, ChronoUnit.MILLIS));
        if (specResults == null)
        {
            return null;
        }

        TimeSeriesSinglePropertyResponse res = new TimeSeriesSinglePropertyResponse();

        res.timestamps = specResults.timestamps;
        res.results    = specResults;

        if (deltaEncode)
        {
            TimeSeriesPropertyResponse.deltaEncode(res.timestamps);
            TimeSeriesPropertyResponse.deltaEncode(res.results.values);

            res.deltaEncoded = true;
        }

        return res;
    }
}
