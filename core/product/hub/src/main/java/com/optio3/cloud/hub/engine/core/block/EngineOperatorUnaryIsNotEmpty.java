/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveBoolean;

@JsonTypeName("EngineOperatorUnaryIsNotEmpty")
public class EngineOperatorUnaryIsNotEmpty extends EngineOperatorUnaryFromCore<EngineValuePrimitiveBoolean, EngineValue>
{
    public EngineOperatorUnaryIsNotEmpty()
    {
        super(EngineValuePrimitiveBoolean.class);
    }

    @Override
    protected EngineValuePrimitiveBoolean computeResult(EngineExecutionContext<?, ?> ctx,
                                                        EngineExecutionStack stack,
                                                        EngineValue val)
    {
        Boolean result = EngineValue.isEmpty(val);
        if (result == null)
        {
            throw stack.unexpected();
        }

        return EngineValuePrimitiveBoolean.create(!result);
    }
}
