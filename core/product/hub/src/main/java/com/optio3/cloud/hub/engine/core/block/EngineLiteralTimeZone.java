/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueTimeZone;

@JsonTypeName("EngineLiteralTimeZone")
public class EngineLiteralTimeZone extends EngineLiteralFromCore<EngineValueTimeZone>
{
    public String value;

    //--//

    public EngineLiteralTimeZone()
    {
        super(EngineValueTimeZone.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(EngineValueTimeZone.create(value));
    }
}
