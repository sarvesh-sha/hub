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
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;

@JsonTypeName("NormalizationEngineStatementSetOutputValue")
public class NormalizationEngineStatementSetOutputValue extends EngineStatementFromNormalization
{
    public EngineExpression<EngineValuePrimitiveString> value;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, value, EngineValuePrimitiveString.class, (valueRaw) ->
        {
            String value = EngineValuePrimitiveString.extract(valueRaw);

            NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;
            ctx2.state.controlPointName = value;

            ctx.popBlock();
        });
    }
}