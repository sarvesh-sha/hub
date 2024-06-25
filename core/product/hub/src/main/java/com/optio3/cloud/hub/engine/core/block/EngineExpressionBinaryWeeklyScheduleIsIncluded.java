/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTime;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveBoolean;
import com.optio3.cloud.hub.engine.core.value.EngineValueTimeZone;
import com.optio3.cloud.hub.engine.core.value.EngineValueWeeklySchedule;

@JsonTypeName("EngineExpressionBinaryWeeklyScheduleIsIncluded")
public class EngineExpressionBinaryWeeklyScheduleIsIncluded extends EngineOperatorBinaryFromCore<EngineValuePrimitiveBoolean, EngineValueWeeklySchedule, EngineValueDateTime>
{
    public EngineExpressionBinaryWeeklyScheduleIsIncluded()
    {
        super(EngineValuePrimitiveBoolean.class);
    }

    @Override
    protected EngineValuePrimitiveBoolean computeResult(EngineExecutionContext<?, ?> ctx,
                                                        EngineExecutionStack stack,
                                                        EngineValueWeeklySchedule schedule,
                                                        EngineValueDateTime dateTime)
    {
        boolean included;

        if (schedule != null && dateTime != null)
        {
            ZonedDateTime target = dateTime.value;

            if (schedule.zoneCreated != null)
            {
                target = target.withZoneSameInstant(EngineValueTimeZone.resolve(stack, schedule.zoneCreated));
            }

            included = schedule.value.isIncluded(target);
        }
        else
        {
            included = false;
        }

        return EngineValuePrimitiveBoolean.create(included);
    }
}
