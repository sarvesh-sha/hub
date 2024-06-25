/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;

@JsonTypeName("EngineInputParameterNumber")
public class EngineInputParameterNumber extends EngineInputParameterFromCore<EngineValuePrimitiveNumber>
{
    public double value;

    //--//

    public EngineInputParameterNumber()
    {
        super(EngineValuePrimitiveNumber.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(EngineValuePrimitiveNumber.create(value));
    }
}
