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

@JsonTypeName("EngineOperatorUnaryRegexGetGroup")
public class EngineOperatorUnaryRegexGetGroup extends EngineOperatorUnaryFromCore<EngineValuePrimitiveString, EngineValueRegexMatch>
{
    public int group;

    public EngineOperatorUnaryRegexGetGroup()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    protected EngineValuePrimitiveString computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       EngineValueRegexMatch regexMatch)
    {
        stack.checkNonNullValue(regexMatch, "No Regular Expression");

        return EngineValuePrimitiveString.create(regexMatch.getGroup(ctx, group));
    }
}
