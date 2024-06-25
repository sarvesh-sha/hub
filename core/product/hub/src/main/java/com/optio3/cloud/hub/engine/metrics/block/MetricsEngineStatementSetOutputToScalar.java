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
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueScalar;

@JsonTypeName("MetricsEngineStatementSetOutputToScalar")
public class MetricsEngineStatementSetOutputToScalar extends EngineStatementFromMetrics
{
    public EngineExpression<MetricsEngineValueScalar> scalar;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, scalar, MetricsEngineValueScalar.class, (scalar) ->
        {
            MetricsEngineExecutionContext ctx2 = (MetricsEngineExecutionContext) ctx;

            ctx2.outputForScalar = scalar;

            ctx.popBlock();
        });
    }
}
