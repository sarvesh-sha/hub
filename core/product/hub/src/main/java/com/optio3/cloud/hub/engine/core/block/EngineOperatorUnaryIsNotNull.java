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

@JsonTypeName("EngineOperatorUnaryIsNotNull")
public class EngineOperatorUnaryIsNotNull extends EngineOperatorUnaryFromCore<EngineValuePrimitiveBoolean, EngineValue>
{
    public EngineOperatorUnaryIsNotNull()
    {
        super(EngineValuePrimitiveBoolean.class);
    }

    @Override
    protected EngineValuePrimitiveBoolean computeResult(EngineExecutionContext<?, ?> ctx,
                                                        EngineExecutionStack stack,
                                                        EngineValue value)
    {
        return EngineValuePrimitiveBoolean.create(value != null);
    }
}
