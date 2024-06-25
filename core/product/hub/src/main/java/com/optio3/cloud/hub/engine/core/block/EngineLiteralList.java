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
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValueListConcrete;
import com.optio3.util.CollectionUtils;

@JsonTypeName("EngineLiteralList")
public class EngineLiteralList extends EngineLiteralFromCore<EngineValueList>
{
    public List<EngineExpression<?>> value;

    //--//

    public EngineLiteralList()
    {
        super(EngineValueList.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ScratchPad scratchPad = stack.getScratchPad(ScratchPad.class);

        EngineExpression<?> expr = CollectionUtils.getNthElement(value, scratchPad.stateMachine);
        if (expr != null)
        {
            ctx.pushBlock(expr);
            scratchPad.stateMachine++;
        }
        else
        {
            EngineValueListConcrete<EngineValue> res = new EngineValueListConcrete<>();
            res.elements.addAll(stack.childResults);
            ctx.popBlock(res);
        }
    }
}
