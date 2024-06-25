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
import com.optio3.cloud.hub.engine.core.value.EngineValueRegexMatch;

@JsonTypeName("EngineExpressionBinaryRegexMatch")
public class EngineExpressionBinaryRegexMatch extends EngineOperatorBinaryFromCore<EngineValueRegexMatch, EngineValuePrimitiveString, EngineValuePrimitiveString>
{
    public EngineExpressionBinaryRegexMatch()
    {
        super(EngineValueRegexMatch.class);
    }

    @Override
    protected EngineValueRegexMatch computeResult(EngineExecutionContext<?, ?> ctx,
                                                  EngineExecutionStack stack,
                                                  EngineValuePrimitiveString regEx,
                                                  EngineValuePrimitiveString input)
    {
        stack.checkNonNullValue(regEx, "No Regular Expression");

        EngineValueRegexMatch regexMatch = EngineValueRegexMatch.create(regEx.value, EngineValuePrimitiveString.extract(input, ""), false);
        return regexMatch.isMatch(ctx) ? regexMatch : null;
    }
}
