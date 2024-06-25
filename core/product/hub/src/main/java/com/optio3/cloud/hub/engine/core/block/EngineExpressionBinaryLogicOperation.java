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

@JsonTypeName("EngineExpressionBinaryLogicOperation")
public class EngineExpressionBinaryLogicOperation extends EngineOperatorBinaryFromCore<EngineValuePrimitiveBoolean, EngineValuePrimitiveBoolean, EngineValuePrimitiveBoolean>
{
    public boolean and;

    public EngineExpressionBinaryLogicOperation()
    {
        super(EngineValuePrimitiveBoolean.class);
    }

    @Override
    protected EngineValuePrimitiveBoolean computeResult(EngineExecutionContext<?, ?> ctx,
                                                        EngineExecutionStack stack,
                                                        EngineValuePrimitiveBoolean primitiveA,
                                                        EngineValuePrimitiveBoolean primitiveB)
    {
        if (and)
        {
            return EngineValuePrimitiveBoolean.create(primitiveA.asBoolean() && primitiveB.asBoolean());
        }
        else
        {
            return EngineValuePrimitiveBoolean.create(primitiveA.asBoolean() || primitiveB.asBoolean());
        }
    }
}
