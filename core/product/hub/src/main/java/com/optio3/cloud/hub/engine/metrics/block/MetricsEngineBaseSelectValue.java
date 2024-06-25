/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.metrics.MetricsEngineExecutionContext;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineSelectValue;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesInterpolate;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;

@JsonSubTypes({ @JsonSubTypes.Type(value = MetricsEngineCreateEnumeratedSeries.class), @JsonSubTypes.Type(value = MetricsEngineCreateMultiStableSeries.class) })
public abstract class MetricsEngineBaseSelectValue extends EngineExpressionFromMetrics<MetricsEngineValueSeries>
{
    public List<EngineExpression<?>> value;

    public MetricsEngineBaseSelectValue()
    {
        super(MetricsEngineValueSeries.class);
    }

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ScratchPad scratchPad = stack.getScratchPad(ScratchPad.class);

        EngineExpression<?> expr = CollectionUtils.getNthElement(value, scratchPad.stateMachine);
        if (expr != null)
        {
            ctx.pushBlock(expr);
            scratchPad.stateMachine++;
        }
        else
        {
            MetricsEngineExecutionContext  ctx2   = (MetricsEngineExecutionContext) ctx;
            List<MetricsEngineSelectValue> inputs = Lists.newArrayList();

            for (EngineValue childResult : stack.childResults)
            {
                var childSelectValue = Reflection.as(childResult, MetricsEngineSelectValue.class);
                if (childSelectValue != null)
                {
                    inputs.add(childSelectValue);
                }
            }

            if (inputs.isEmpty())
            {
                ctx.popBlock(null);
                return;
            }

            var results = TimeSeriesInterpolate.execute(ctx2.maxInterpolationGap, CollectionUtils.transformToList(inputs, v -> v.values));
            int samples = results.timestamps.length;

            TimeSeriesPropertyResponse res = new TimeSeriesPropertyResponse();
            res.timestamps = results.timestamps;

            res.setUnits(EngineeringUnits.enumerated);

            res.values = new double[samples];
            Arrays.fill(res.values, Double.NaN);

            res.enumLookup = new String[inputs.size()];

            for (int inputIndex = 0; inputIndex < inputs.size(); inputIndex++)
            {
                MetricsEngineSelectValue selectValue = inputs.get(inputIndex);
                res.enumLookup[inputIndex] = selectValue.identifier;
            }

            fillValues(res, inputs);

            ctx.popBlock(new MetricsEngineValueSeries(res));
        }
    }

    protected abstract void fillValues(TimeSeriesPropertyResponse res,
                                       List<MetricsEngineSelectValue> inputs);
}
