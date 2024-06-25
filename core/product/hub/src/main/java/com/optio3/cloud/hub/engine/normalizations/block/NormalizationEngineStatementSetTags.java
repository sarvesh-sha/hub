/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.serialization.Reflection;

@JsonTypeName("NormalizationEngineStatementSetTags")
public class NormalizationEngineStatementSetTags extends EngineStatementFromNormalization
{
    public EngineExpression<EngineValueList<EngineValuePrimitiveString>> value;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;
        extractParams(ctx, stack, value, EngineValueList.class, (valueRaw) ->
        {
            ctx2.state.tagsSet = true;
            ctx2.state.tags.clear();

            if (valueRaw != null)
            {
                for (int i = 0; i < valueRaw.getLength(); i++)
                {
                    EngineValuePrimitiveString tag = Reflection.as(valueRaw.getNthElement(i), EngineValuePrimitiveString.class);
                    if (tag != null)
                    {
                        ctx2.state.tags.add(tag.value);
                    }
                }
            }

            ctx.popBlock();
        });
    }
}
