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
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSetOfSeries;

@JsonTypeName("MetricsEngineCreateVector3")
public class MetricsEngineCreateVector3 extends EngineExpressionFromMetrics<MetricsEngineValueSetOfSeries>
{
    public EngineExpression<MetricsEngineValueSeries> xSeries;
    public EngineExpression<MetricsEngineValueSeries> ySeries;
    public EngineExpression<MetricsEngineValueSeries> zSeries;

    //--//

    public MetricsEngineCreateVector3()
    {
        super(MetricsEngineValueSetOfSeries.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        extractParams(ctx, stack, xSeries, MetricsEngineValueSeries.class, ySeries, MetricsEngineValueSeries.class, zSeries, MetricsEngineValueSeries.class, (x, y, z) ->
        {
            MetricsEngineValueSetOfSeries res = new MetricsEngineValueSetOfSeries();
            res.elements.add(x);
            res.elements.add(y);
            res.elements.add(z);

            ctx.popBlock(res.convertToSameUnits(true));
        });
    }
}
