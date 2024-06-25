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
import com.optio3.cloud.hub.engine.EngineStatement;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.EngineConditionBlock;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitive;
import com.optio3.util.CollectionUtils;

@JsonTypeName("EngineStatementLogicIf")
public class EngineStatementLogicIf extends EngineStatementFromCore
{
    public static class ScratchPadImpl extends ScratchPad
    {
        int blockIndex;
        int statementIndex;
    }

    public List<EngineConditionBlock> ifElseBlocks = Lists.newArrayList();

    public List<EngineStatement> elseStatements;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ScratchPadImpl scratchPad = stack.getScratchPad(ScratchPadImpl.class);

        switch (scratchPad.stateMachine)
        {
            case 0: // Go through branches, evaluating the conditions.
            {
                if (scratchPad.blockIndex < ifElseBlocks.size())
                {
                    EngineConditionBlock cond = ifElseBlocks.get(scratchPad.blockIndex);
                    ctx.pushBlock(cond.condition);
                    scratchPad.stateMachine = 1;
                }
                else
                {
                    scratchPad.stateMachine = 2;
                }
            }
            return;

            case 1: // Is it true?
            {
                EngineValue condition = stack.popChildResult(EngineValue.class);

                if (!EngineValuePrimitive.isTrue(condition)) // Try next condition.
                {
                    scratchPad.blockIndex++;
                    scratchPad.stateMachine = 0;
                }
                else
                {
                    scratchPad.stateMachine = 2;
                }
            }
            return;

            case 2: // Execute statements in the selected branch.
            {
                List<EngineStatement> statements = scratchPad.blockIndex < ifElseBlocks.size() ? ifElseBlocks.get(scratchPad.blockIndex).statements : elseStatements;

                EngineStatement stmt = CollectionUtils.getNthElement(statements, scratchPad.statementIndex);
                if (stmt != null)
                {
                    ctx.pushBlock(stmt);
                    scratchPad.statementIndex++;
                }
                else
                {
                    ctx.popBlock();
                }
            }
            return;
        }

        throw stack.unexpected();
    }
}