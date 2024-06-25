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
import com.optio3.cloud.hub.engine.metrics.MetricsEngineExecutionContext;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;

@JsonTypeName("MetricsEngineStatementSetOutputToSeriesWithName")
public class MetricsEngineStatementSetOutputToSeriesWithName extends EngineStatementFromMetrics
{
    public String name;

    public EngineExpression<MetricsEngineValueSeries> series;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, series, MetricsEngineValueSeries.class, (series) ->
        {
            MetricsEngineExecutionContext ctx2 = (MetricsEngineExecutionContext) ctx;

            ctx2.outputForNamedSeries.put(name, series);

            ctx.popBlock();
        });
    }
}
