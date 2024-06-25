/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.alerts.block.EngineOperatorUnaryFromAlerts;
import com.optio3.cloud.hub.engine.metrics.block.EngineOperatorUnaryFromMetrics;
import com.optio3.cloud.hub.engine.normalizations.block.EngineOperatorUnaryFromNormalization;

@JsonSubTypes({ @JsonSubTypes.Type(value = EngineOperatorUnaryFromCore.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryFromAlerts.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryFromMetrics.class),
                @JsonSubTypes.Type(value = EngineOperatorUnaryFromNormalization.class) })
public abstract class EngineOperatorUnary<To extends EngineValue, Ti extends EngineValue> extends EngineExpressionFromCore<To>
{
    public EngineExpression<Ti> a;

    //--//

    protected EngineOperatorUnary(Class<To> resultType)
    {
        super(resultType);
    }

    protected EngineOperatorUnary(TypeReference<To> resultType)
    {
        super(resultType);
    }


    @Override
    public final void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                       EngineExecutionStack stack) throws
                                                                   Exception
    {
        extractParams(ctx, stack, a, a.resultType, (valueA) ->
        {
            ctx.popBlock(computeResult(ctx, stack, valueA));
        });
    }

    protected abstract To computeResult(EngineExecutionContext<?, ?> ctx,
                                        EngineExecutionStack stack,
                                        Ti valueA) throws
                                                   Exception;
}
