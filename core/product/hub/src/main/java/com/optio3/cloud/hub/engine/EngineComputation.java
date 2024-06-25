/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.core.block.EngineProcedureDeclaration;
import com.optio3.cloud.hub.engine.core.block.EngineThread;
import com.optio3.util.CollectionUtils;

@JsonSubTypes({ @JsonSubTypes.Type(value = EngineThread.class), @JsonSubTypes.Type(value = EngineProcedureDeclaration.class) })
public abstract class EngineComputation extends EngineBlock
{
    // TODO: UPGRADE PATCH: Legacy fixup to remove unused variables field
    public void setVariables(List<EngineVariable> variables)
    {
    }

    public List<EngineStatement> statements;

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
