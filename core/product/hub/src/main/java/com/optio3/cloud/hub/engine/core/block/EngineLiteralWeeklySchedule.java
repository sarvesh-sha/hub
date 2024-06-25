/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueWeeklySchedule;
import com.optio3.cloud.hub.model.schedule.RecurringWeeklySchedule;

@JsonTypeName("EngineLiteralWeeklySchedule")
public class EngineLiteralWeeklySchedule extends EngineLiteralFromCore<EngineValueWeeklySchedule>
{
    public RecurringWeeklySchedule value;

    //--//

    public EngineLiteralWeeklySchedule()
    {
        super(EngineValueWeeklySchedule.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(EngineValueWeeklySchedule.create(value, null));
    }
}
