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

@JsonTypeName("EngineOperatorUnaryIsNull")
public class EngineOperatorUnaryIsNull extends EngineOperatorUnaryFromCore<EngineValuePrimitiveBoolean, EngineValue>
{
    public EngineOperatorUnaryIsNull()
    {
        super(EngineValuePrimitiveBoolean.class);
    }

    @Override
    protected EngineValuePrimitiveBoolean computeResult(EngineExecutionContext<?, ?> ctx,
                                                        EngineExecutionStack stack,
                                                        EngineValue value)
    {
        return EngineValuePrimitiveBoolean.create(value == null);
    }
}
