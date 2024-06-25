/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitive;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;

@JsonTypeName("EngineExpressionBinaryStringConcat")
public class EngineExpressionBinaryStringConcat extends EngineOperatorBinaryFromCore<EngineValuePrimitiveString, EngineValuePrimitive, EngineValuePrimitive>
{
    public EngineExpressionBinaryStringConcat()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    protected EngineValuePrimitiveString computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       EngineValuePrimitive v1,
                                                       EngineValuePrimitive v2)
    {
        String t1 = v1 != null ? v1.asString() : "";
        String t2 = v2 != null ? v2.asString() : "";

        return EngineValuePrimitiveString.create(t1 + t2);
    }
}
