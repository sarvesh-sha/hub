/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueScalar;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.protocol.model.EngineeringUnits;

@JsonTypeName("MetricsEngineOperatorThresholdRange")
public class MetricsEngineOperatorThresholdRange extends EngineExpressionFromMetrics<MetricsEngineValueSeries>
{
    public EngineExpression<MetricsEngineValueSeries> series;
    public EngineExpression<MetricsEngineValueScalar> lowerBound;
    public EngineExpression<MetricsEngineValueScalar> upperBound;
    public boolean                                    inclusive;
    public boolean                                    invert;

    //--//

    public MetricsEngineOperatorThresholdRange()
    {
        super(MetricsEngineValueSeries.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        extractParams(ctx, stack, series, MetricsEngineValueSeries.class, lowerBound, MetricsEngineValueScalar.class, upperBound, MetricsEngineValueScalar.class, (series, lowerBound, upperBound) ->
        {
            stack.checkNonNullValue(series, "Missing series parameter");
            stack.checkNonNullValue(lowerBound, "Missing lowerBound parameter");
            stack.checkNonNullValue(upperBound, "Missing upperBound parameter");

            MetricsEngineValueSeries res = series.copy();
            res.setUnitsFactors(EngineeringUnits.activeInactive);

            double   thresholdLower = EngineeringUnits.convert(lowerBound.value, lowerBound.units, series.getUnitsFactors());
            double   thresholdUpper = EngineeringUnits.convert(upperBound.value, upperBound.units, series.getUnitsFactors());
            double[] inputs         = series.values.values;
            double[] outputs        = res.values.values;

            for (int i = 0; i < inputs.length; i++)
            {
                boolean active;
                double  value = inputs[i];

                if (inclusive)
                {
                    active = (thresholdLower <= value && value <= thresholdUpper);
                }
                else
                {
                    active = (thresholdLower < value && value < thresholdUpper);
                }

                if (invert)
                {
                    active = !active;
                }

                outputs[i] = active ? 1.0 : 0.0;
            }

            ctx.popBlock(res);
        });
    }
}
