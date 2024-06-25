/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.EngineStatement;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitive;
import com.optio3.util.CollectionUtils;

@JsonTypeName("EngineStatementRepeatWhile")
public class EngineStatementRepeatWhile extends EngineStatementFromCore
{
    public static class ScratchPadImpl extends ScratchPad
    {
        int statementPos;
    }

    //--//

    public EngineExpression<EngineValuePrimitive> condition;

    public List<EngineStatement> statements = Lists.newArrayList();

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ScratchPadImpl scratchPad = stack.getScratchPad(ScratchPadImpl.class);

        switch (scratchPad.stateMachine)
        {
            case 0: // Evaluate condition
            {
                ctx.pushBlock(condition);
                scratchPad.stateMachine = 1;
            }
            return;

            case 1: // Is it true?
            {
                EngineValuePrimitive res = stack.popChildResult(EngineValuePrimitive.class);
                if (!res.asBoolean())
                {
                    ctx.popBlock();
                }
                else
                {
                    scratchPad.statementPos = 0;
                    scratchPad.stateMachine = 2;
                }
            }
            return;

            case 2: // Execute statements in the selected branch.
            {
                EngineStatement stmt = CollectionUtils.getNthElement(statements, scratchPad.statementPos);
                if (stmt != null)
                {
                    ctx.pushBlock(stmt);
                    scratchPad.statementPos++;
                }
                else
                {
                    scratchPad.stateMachine = 0; // Go back to evaluation the condition.
                }
            }
            return;
        }

        throw stack.unexpected();
    }

    @Override
    public boolean handleLoopRequest(EngineExecutionContext<?, ?> ctx,
                                     EngineExecutionStack stack,
                                     boolean shouldBreak)
    {
        if (shouldBreak)
        {
            ctx.popBlock();
        }
        else
        {
            ScratchPadImpl scratchPad = stack.getScratchPad(ScratchPadImpl.class);
            scratchPad.stateMachine = 0; // Go back to evaluation the condition.
        }

        return true;
    }
}
