/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Maps;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.util.CollectionUtils;

@JsonTypeName("EngineExpressionFunctionCall")
public class EngineExpressionFunctionCall extends EngineExpressionFromCore<EngineValue>
{
    public String functionId;

    // TODO: UPGRADE PATCH: Legacy fixup to remove unused functionName field
    public void setFunctionName(String functionName)
    {
        functionId = functionName;
    }

    public List<EngineVariableAssignment> arguments;

    @JsonIgnore
    public EngineProcedureDeclaration resolvedFunction;

    //--//

    public EngineExpressionFunctionCall()
    {
        super(EngineValue.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ScratchPad scratchPad = stack.getScratchPad(ScratchPad.class);

        switch (scratchPad.stateMachine)
        {
            case 0:
                //
                // Create a new local scope, initializing it with all the local variables.
                //
                stack.localVariablesSetter = Maps.newHashMap();

                if (arguments != null)
                {
                    for (EngineVariableAssignment argument : arguments)
                    {
                        stack.localVariablesSetter.put(argument.variable.name, null);
                    }
                }

                // Fallthrough
            default:
            {
                EngineVariableAssignment stmt = CollectionUtils.getNthElement(arguments, scratchPad.stateMachine);
                if (stmt != null)
                {
                    ctx.pushBlock(stmt);
                    scratchPad.stateMachine++;
                }
                else
                {
                    //
                    // Now that we have all the arguments' values, expose the scope to the code.
                    //
                    stack.localVariablesGetter = stack.localVariablesSetter;
                    ctx.pushCall(this);
                    scratchPad.stateMachine = -1;
                }
            }
            return;

            case -1:
                ctx.popBlock(stack.popChildResult(EngineValue.class));
                return;
        }
    }
}
