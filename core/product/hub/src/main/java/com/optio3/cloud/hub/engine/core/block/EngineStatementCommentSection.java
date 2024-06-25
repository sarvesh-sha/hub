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
import com.optio3.util.CollectionUtils;

@JsonTypeName("EngineStatementCommentSection")
public class EngineStatementCommentSection extends EngineStatementFromCore
{
    public String text;

    public List<EngineStatement> statements = Lists.newArrayList();

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ScratchPad scratchPad = stack.getScratchPad(ScratchPad.class);

        EngineStatement stmt = CollectionUtils.getNthElement(statements, scratchPad.stateMachine);
        if (stmt != null)
        {
            ctx.pushBlock(stmt);
            scratchPad.stateMachine++;
        }
        else
        {
            ctx.popBlock();
        }
    }
}
