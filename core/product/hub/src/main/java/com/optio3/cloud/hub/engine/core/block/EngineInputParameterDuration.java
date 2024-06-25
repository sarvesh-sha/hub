/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueDuration;

@JsonTypeName("EngineInputParameterDuration")
public class EngineInputParameterDuration extends EngineInputParameterFromCore<EngineValueDuration>
{
    public int        amount;
    public ChronoUnit unit;

    //--//

    public EngineInputParameterDuration()
    {
        super(EngineValueDuration.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(EngineValueDuration.create(amount, unit));
    }
}
