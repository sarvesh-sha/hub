/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.timeseries;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.logic.samples.SamplesCache;

public class TimeSeriesMultiPropertyRequest extends TimeSeriesBaseRequest
{
    public List<TimeSeriesPropertyRequest> specs;

    public double maxInterpolationGap;

    public boolean deltaEncode;

    //--//

    public TimeSeriesMultiPropertyResponse fetch(SamplesCache cache)
    {
        List<TimeSeriesPropertyResponse> inputs = Lists.newArrayList();

        for (TimeSeriesPropertyRequest spec : specs)
        {
            TimeSeriesPropertyResponse specResults = spec.fetch(cache, this, Duration.of(100, ChronoUnit.MILLIS));
            if (specResults == null)
            {
                // Only interpolate if all series have values.
                return null;
            }

            inputs.add(specResults);
        }

        TimeSeriesMultiPropertyResponse res = TimeSeriesInterpolate.execute(maxInterpolationGap, inputs);

        if (deltaEncode)
        {
            TimeSeriesPropertyResponse.deltaEncode(res.timestamps);

            for (TimeSeriesPropertyResponse subRes : res.results)
            {
                TimeSeriesPropertyResponse.deltaEncode(subRes.values);
            }

            res.deltaEncoded = true;
        }

        return res;
    }
}
