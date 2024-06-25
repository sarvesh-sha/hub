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
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;

@JsonTypeName("EngineOperatorUnaryIsNotValidNumber")
public class EngineOperatorUnaryIsNotValidNumber extends EngineOperatorUnaryFromCore<EngineValuePrimitiveBoolean, EngineValuePrimitiveNumber>
{
    public EngineOperatorUnaryIsNotValidNumber()
    {
        super(EngineValuePrimitiveBoolean.class);
    }

    @Override
    protected EngineValuePrimitiveBoolean computeResult(EngineExecutionContext<?, ?> ctx,
                                                        EngineExecutionStack stack,
                                                        EngineValuePrimitiveNumber value)
    {
        return EngineValuePrimitiveBoolean.create(!EngineValuePrimitiveNumber.isValidNumber(value));
    }
}
