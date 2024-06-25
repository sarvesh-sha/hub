/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.EngineVariableReference;

@JsonTypeName("EngineExpressionGetVariable")
public class EngineExpressionGetVariable extends EngineExpressionFromCore<EngineValue>
{
    public EngineVariableReference variable;

    public EngineExpressionGetVariable()
    {
        super(EngineValue.class);
    }

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(ctx.getVariable(variable));
    }
}
