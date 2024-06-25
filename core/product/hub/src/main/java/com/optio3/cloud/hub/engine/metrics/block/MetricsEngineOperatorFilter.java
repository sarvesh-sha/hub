/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.model.shared.program.CommonEngineAggregateOperation;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesInterpolate;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;

@JsonTypeName("MetricsEngineOperatorFilter")
public class MetricsEngineOperatorFilter extends EngineOperatorBinaryFromMetrics<MetricsEngineValueSeries, MetricsEngineValueSeries, MetricsEngineValueSeries>
{
    public CommonEngineAggregateOperation operation;

    public MetricsEngineOperatorFilter()
    {
        super(MetricsEngineValueSeries.class);
    }

    @Override
    protected MetricsEngineValueSeries computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     MetricsEngineValueSeries series,
                                                     MetricsEngineValueSeries filter)
    {
        if (series == null)
        {
            throw stack.unexpected("Missing input parameter");
        }

        if (filter == null)
        {
            throw stack.unexpected("Missing filter parameter");
        }

        TimeSeriesPropertyResponse[] dimensionsInterpolated = TimeSeriesInterpolate.execute(10 * 60, series.values, filter.values);
        double[]                     seriesRaw              = dimensionsInterpolated[0].values;
        double[]                     filterRaw              = dimensionsInterpolated[1].values;

        for (int i = 0; i < seriesRaw.length; i++)
        {
            if (filterRaw[i] <= 0.0)
            {
                seriesRaw[i] = Double.NaN;
            }
        }

        return new MetricsEngineValueSeries(dimensionsInterpolated[0]);
    }
}

