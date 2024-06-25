/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveBoolean;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.core.value.EngineValueRegexMatch;

@JsonTypeName("EngineExpressionBinaryStringIsMatch")
public class EngineExpressionBinaryStringIsMatch extends EngineOperatorBinaryFromCore<EngineValuePrimitiveBoolean, EngineValuePrimitiveString, EngineValuePrimitiveString>
{
    public EngineExpressionBinaryStringIsMatch()
    {
        super(EngineValuePrimitiveBoolean.class);
    }

    @Override
    protected EngineValuePrimitiveBoolean computeResult(EngineExecutionContext<?, ?> ctx,
                                                        EngineExecutionStack stack,
                                                        EngineValuePrimitiveString regEx,
                                                        EngineValuePrimitiveString input)
    {
        stack.checkNonNullValue(regEx, "No Regular Expression");

        EngineValueRegexMatch regexMatch = EngineValueRegexMatch.create(regEx.value, EngineValuePrimitiveString.extract(input, ""), false);

        return EngineValuePrimitiveBoolean.create(regexMatch.isMatch(ctx));
    }
}
