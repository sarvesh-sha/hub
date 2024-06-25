/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import java.util.function.BiFunction;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueScalar;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.model.shared.program.CommonEngineAggregateOperation;
import com.optio3.cloud.hub.persistence.asset.TimeSeries;
import com.optio3.util.BoxingUtils;

@JsonTypeName("MetricsEngineOperatorAggregate")
public class MetricsEngineOperatorAggregate extends EngineOperatorUnaryFromMetrics<MetricsEngineValueScalar, MetricsEngineValueSeries>
{
    public CommonEngineAggregateOperation operation;

    public MetricsEngineOperatorAggregate()
    {
        super(MetricsEngineValueScalar.class);
    }

    @Override
    protected MetricsEngineValueScalar computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     MetricsEngineValueSeries series)
    {
        if (series == null)
        {
            throw stack.unexpected("Missing input parameter");
        }

        switch (operation)
        {
            case Min:
                return process(series, (timestamps, values) ->
                {
                    double min = Double.NaN;

                    for (int i = 0; i < values.length; i++)
                    {
                        double value = values[i];

                        min = BoxingUtils.minWithNaN(value, min);
                    }

                    return min;
                });

            case Max:
                return process(series, (timestamps, values) ->
                {
                    double max = Double.NaN;

                    for (int i = 0; i < values.length; i++)
                    {
                        double value = values[i];

                        max = BoxingUtils.maxWithNaN(value, max);
                    }

                    return max;
                });

            case Mean:
                TimeSeries.NumericValueRanges summary = series.toRanges();
                long timeSum = 0;
                double valueSum = 0;

                for (TimeSeries.NumericValueRange valueRange : summary.ranges)
                {
                    timeSum += valueRange.delta;
                    valueSum += valueRange.delta * (valueRange.valueLeft + valueRange.valueRight) / 2;
                }

                MetricsEngineValueScalar res = new MetricsEngineValueScalar();
                res.units = series.getUnitsFactors();
                res.value = timeSum > 0 ? valueSum / timeSum : Double.NaN;
                return res;

            default:
                throw stack.unexpected("Invalid operation: %s", operation);
        }
    }

    private static MetricsEngineValueScalar process(MetricsEngineValueSeries input,
                                                    BiFunction<double[], double[], Double> callback)
    {
        MetricsEngineValueScalar res = new MetricsEngineValueScalar();
        res.units = input.getUnitsFactors();
        res.value = callback.apply(input.values.timestamps, input.values.values);

        return res;
    }
}

