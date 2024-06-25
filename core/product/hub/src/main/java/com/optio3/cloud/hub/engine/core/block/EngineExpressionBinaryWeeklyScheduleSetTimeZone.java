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
import com.optio3.cloud.hub.engine.core.value.EngineValueWeeklySchedule;

@JsonTypeName("EngineExpressionBinaryWeeklyScheduleSetTimeZone")
public class EngineExpressionBinaryWeeklyScheduleSetTimeZone extends EngineOperatorBinaryFromCore<EngineValueWeeklySchedule, EngineValueWeeklySchedule, EngineValueTimeZone>
{
    public EngineExpressionBinaryWeeklyScheduleSetTimeZone()
    {
        super(EngineValueWeeklySchedule.class);
    }

    @Override
    protected EngineValueWeeklySchedule computeResult(EngineExecutionContext<?, ?> ctx,
                                                      EngineExecutionStack stack,
                                                      EngineValueWeeklySchedule schedule,
                                                      EngineValueTimeZone timeZone)
    {
        if (schedule == null)
        {
            return null;
        }

        EngineValueWeeklySchedule result = schedule;

        if (timeZone != null)
        {
            result = EngineValueWeeklySchedule.create(schedule.value, timeZone.value);
        }

        return result;
    }
}
