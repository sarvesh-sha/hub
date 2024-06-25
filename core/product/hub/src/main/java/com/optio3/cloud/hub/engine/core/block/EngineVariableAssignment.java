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
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.EngineVariable;

@JsonTypeName("EngineVariableAssignment")
public class EngineVariableAssignment extends EngineStatementFromCore
{
    public EngineVariable variable;

    public EngineExpression<? extends EngineValue> value;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, value, EngineValue.class, (value) ->
        {
            ctx.assignVariable(variable, value);
            ctx.popBlock();
        });
    }
}
