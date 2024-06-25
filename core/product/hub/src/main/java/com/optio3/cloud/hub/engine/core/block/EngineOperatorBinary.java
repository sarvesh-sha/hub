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
import com.optio3.cloud.hub.engine.alerts.block.EngineOperatorBinaryFromAlerts;
import com.optio3.cloud.hub.engine.metrics.block.EngineOperatorBinaryFromMetrics;
import com.optio3.cloud.hub.engine.normalizations.block.EngineOperatorBinaryFromNormalization;

@JsonSubTypes({ @JsonSubTypes.Type(value = EngineOperatorBinaryFromCore.class),
                @JsonSubTypes.Type(value = EngineOperatorBinaryFromAlerts.class),
                @JsonSubTypes.Type(value = EngineOperatorBinaryFromMetrics.class),
                @JsonSubTypes.Type(value = EngineOperatorBinaryFromNormalization.class) })
public abstract class EngineOperatorBinary<To extends EngineValue, Ta extends EngineValue, Tb extends EngineValue> extends EngineExpressionFromCore<To>
{
    public EngineExpression<Ta> a;
    public EngineExpression<Tb> b;

    //--//

    protected EngineOperatorBinary(Class<To> resultType)
    {
        super(resultType);
    }

    protected EngineOperatorBinary(TypeReference<To> resultType)
    {
        super(resultType);
    }

    @Override
    public final void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                       EngineExecutionStack stack) throws
                                                                   Exception
    {
        extractParams(ctx, stack, a, a.resultType, b, b.resultType, (valueA, valueB) ->
        {
            ctx.popBlock(computeResult(ctx, stack, valueA, valueB));
        });
    }

    protected abstract To computeResult(EngineExecutionContext<?, ?> ctx,
                                        EngineExecutionStack stack,
                                        Ta valueA,
                                        Tb valueB);
}
