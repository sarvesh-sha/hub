/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveBoolean;

@JsonTypeName("EngineExpressionMemoize")
public class EngineExpressionMemoize extends EngineExpressionFromCore<EngineValue>
{
    public EngineExpression<?> value;

    public EngineExpressionMemoize()
    {
        super(EngineValue.class);
    }

    @Override
    public final void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                       EngineExecutionStack stack) throws
                                                                   Exception
    {
        if (ctx.memoizer.containsKey(this))
        {
            ctx.popBlock(ctx.memoizer.get(this));
        }
        else
        {
            extractParams(ctx, stack, value, value.resultType, (valueA) ->
            {
                ctx.memoizer.put(this, valueA);
                ctx.popBlock(valueA);
            });
        }
    }
}
