/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.value;

import java.time.ZoneId;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.model.schedule.RecurringWeeklySchedule;

@JsonTypeName("EngineValueWeeklySchedule")
public class EngineValueWeeklySchedule extends EngineValue
{
    public static final ZoneId c_defaultZone = ZoneId.of("America/Los_Angeles");

    public RecurringWeeklySchedule value;
    public String                  zoneCreated;

    public static EngineValueWeeklySchedule create(RecurringWeeklySchedule val,
                                                   String zoneCreated)
    {
        if (val == null)
        {
            return null;
        }

        EngineValueWeeklySchedule res = new EngineValueWeeklySchedule();
        res.value       = val;
        res.zoneCreated = zoneCreated;
        return res;
    }

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return null;
    }
}
