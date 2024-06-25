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
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.EngineVariable;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValueListIterator;
import com.optio3.util.CollectionUtils;

@JsonTypeName("EngineStatementForEach")
public class EngineStatementForEach extends EngineStatementFromCore
{
    public static class ScratchPadImpl extends ScratchPad
    {
        EngineValueListIterator<? extends EngineValue> iterator;
        int                                            statementIndex;
    }

    //--//

    public EngineExpression<EngineValueList<?>> list;

    public EngineVariable variable;

    public List<EngineStatement> statements = Lists.newArrayList();

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ScratchPadImpl scratchPad = stack.getScratchPad(ScratchPadImpl.class);

        switch (scratchPad.stateMachine)
        {
            case 0:
            {
                ctx.pushBlock(list);
                scratchPad.stateMachine = 1;
            }
            return;

            case 1:
            {
                EngineValueList<?> list = stack.popChildResult(EngineValueList.class);
                if (list != null)
                {
                    scratchPad.iterator = list.createIterator();
                }
                scratchPad.stateMachine = 2;
            }
            return;

            case 2:
            {
                if (scratchPad.iterator == null || !scratchPad.iterator.hasNext())
                {
                    ctx.popBlock();
                }
                else
                {
                    ctx.assignVariable(variable, scratchPad.iterator.next());
                    scratchPad.statementIndex = 0;
                    scratchPad.stateMachine   = 3;
                }
            }
            return;

            case 3:
            {
                EngineStatement stmt = CollectionUtils.getNthElement(statements, scratchPad.statementIndex);
                if (stmt != null)
                {
                    ctx.pushBlock(stmt);
                    scratchPad.statementIndex++;
                }
                else
                {
                    scratchPad.stateMachine = 2; // Back to the next item in the list.
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
            ScratchPad scratchPad = stack.getScratchPad(ScratchPad.class);

            scratchPad.stateMachine = 2; // Back to the next item in the list.
        }

        return true;
    }
}
