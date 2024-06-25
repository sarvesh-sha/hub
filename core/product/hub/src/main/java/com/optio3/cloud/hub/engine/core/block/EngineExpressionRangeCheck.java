/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitive;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveBoolean;

@JsonTypeName("EngineExpressionRangeCheck")
public class EngineExpressionRangeCheck extends EngineExpressionFromCore<EngineValuePrimitiveBoolean>
{
    public EngineExpression<EngineValuePrimitive> value;
    public EngineExpression<EngineValuePrimitive> minRange;
    public EngineExpression<EngineValuePrimitive> maxRange;

    public EngineExpressionRangeCheck()
    {
        super(EngineValuePrimitiveBoolean.class);
    }

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        extractParams(ctx, stack, value, EngineValuePrimitive.class, minRange, EngineValuePrimitive.class, maxRange, EngineValuePrimitive.class, (val, minRange, maxRange) ->
        {
            boolean inRange;

            if (val == null)
            {
                inRange = false;
            }
            else
            {
                inRange = val.compareTo(ctx, stack, minRange) >= 0 && val.compareTo(ctx, stack, maxRange) <= 0;
            }

            ctx.popBlock(EngineValuePrimitiveBoolean.create(inRange));
        });
    }
}
