/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitive;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.util.CollectionUtils;

@JsonTypeName("EngineStatementLog")
public class EngineStatementLog extends EngineStatementFromCore
{
    public EngineExpression<EngineValuePrimitive> format;
    public List<EngineExpression<?>>              arguments;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ScratchPad scratchPad = stack.getScratchPad(ScratchPad.class);

        switch (scratchPad.stateMachine)
        {
            case 0:
            {
                ctx.pushBlock(format);
                scratchPad.stateMachine = 1;
            }
            return;

            default:
            {
                EngineExpression<?> expr = CollectionUtils.getNthElement(arguments, scratchPad.stateMachine - 1);
                if (expr != null)
                {
                    ctx.pushBlock(expr);
                    scratchPad.stateMachine++;
                }
                else
                {
                    String formatText = EngineValuePrimitiveString.extract(stack.popChildResult(EngineValuePrimitiveString.class));
                    ctx.recordLogEntry(stack, ctx.format(stack, formatText, stack.childResults));

                    ctx.popBlock();
                }
            }
        }
    }
}
