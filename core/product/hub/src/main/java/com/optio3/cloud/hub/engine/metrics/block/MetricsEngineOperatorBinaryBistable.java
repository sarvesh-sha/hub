/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.metrics.MetricsEngineExecutionContext;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesInterpolate;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;
import com.optio3.protocol.model.EngineeringUnits;

@JsonTypeName("MetricsEngineOperatorBinaryBistable")
public class MetricsEngineOperatorBinaryBistable extends EngineOperatorBinaryFromMetrics<MetricsEngineValueSeries, MetricsEngineValueSeries, MetricsEngineValueSeries>
{
    public MetricsEngineOperatorBinaryBistable()
    {
        super(MetricsEngineValueSeries.class);
    }

    @Override
    protected MetricsEngineValueSeries computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     MetricsEngineValueSeries seriesSet,
                                                     MetricsEngineValueSeries seriesReset)
    {
        stack.checkNonNullValue(seriesSet, "Missing set parameter");
        stack.checkNonNullValue(seriesReset, "Missing reset parameter");

        MetricsEngineExecutionContext ctx2 = (MetricsEngineExecutionContext) ctx;

        List<TimeSeriesPropertyResponse> inputs = com.google.common.collect.Lists.newArrayList();
        inputs.add(seriesSet.values);
        inputs.add(seriesReset.values);

        var results      = TimeSeriesInterpolate.execute(ctx2.maxInterpolationGap, inputs);
        int numOfSamples = results.timestamps.length;

        TimeSeriesPropertyResponse res = new TimeSeriesPropertyResponse();
        res.timestamps = results.timestamps;
        res.values     = new double[numOfSamples];

        res.setUnits(EngineeringUnits.activeInactive);

        double   value       = Double.NaN;
        double[] valuesSet   = results.results[0].values;
        double[] valuesReset = results.results[1].values;

        for (int sampleIndex = 0; sampleIndex < numOfSamples; sampleIndex++)
        {
            if (valuesSet[sampleIndex] > 0.0)
            {
                // Transition to the "active" value at the match with set.
                value = 1;
            }
            else if (valuesReset[sampleIndex] > 0.0)
            {
                // Transition to the "inactive" value at the match with reset.
                value = 0;
            }

            res.values[sampleIndex] = value;
        }

        return new MetricsEngineValueSeries(res);
    }
}
