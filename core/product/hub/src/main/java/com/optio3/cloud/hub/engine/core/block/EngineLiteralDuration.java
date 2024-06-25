/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueDuration;

@JsonTypeName("EngineLiteralDuration")
public class EngineLiteralDuration extends EngineLiteralFromCore<EngineValueDuration>
{
    // TODO: UPGRADE PATCH: Remove after deployed to production.
    public static class Duration
    {
        public int        value;
        public ChronoUnit unit;
    }

    public int        amount;
    public ChronoUnit unit;

    //--//

    public EngineLiteralDuration()
    {
        super(EngineValueDuration.class);
    }

    // TODO: UPGRADE PATCH: Remove after deployed to production.
    public void setValue(Duration val)
    {
        HubApplication.reportPatchCall(val);

        if (val != null)
        {
            amount = val.value;
            unit   = val.unit;
        }
    }

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(EngineValueDuration.create(amount, unit));
    }
}
