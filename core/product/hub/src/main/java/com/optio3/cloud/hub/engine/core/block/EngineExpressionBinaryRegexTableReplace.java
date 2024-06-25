/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.core.value.EngineValueRegexReplaceTable;

@JsonTypeName("EngineExpressionBinaryRegexTableReplace")
public class EngineExpressionBinaryRegexTableReplace extends EngineOperatorBinaryFromCore<EngineValuePrimitiveString, EngineValueRegexReplaceTable, EngineValuePrimitiveString>
{
    public EngineExpressionBinaryRegexTableReplace()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    protected EngineValuePrimitiveString computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       EngineValueRegexReplaceTable regEx,
                                                       EngineValuePrimitiveString replacement)
    {
        stack.checkNonNullValue(regEx, "No Regular Expression Table");

        return EngineValuePrimitiveString.create(regEx.replace(ctx, EngineValuePrimitiveString.extract(replacement, "")));
    }
}
