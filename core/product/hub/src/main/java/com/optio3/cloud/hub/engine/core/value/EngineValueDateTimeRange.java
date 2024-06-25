/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.value;

import java.time.ZonedDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;

@JsonTypeName("EngineValueDateTimeRange")
public class EngineValueDateTimeRange extends EngineValue
{
    public ZonedDateTime start;
    public ZonedDateTime end;

    //--//

    public static EngineValueDateTimeRange create(ZonedDateTime start,
                                                  ZonedDateTime end)
    {
        EngineValueDateTimeRange res = new EngineValueDateTimeRange();
        res.start = start;
        res.end   = end;
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
        String startText = EngineValueDateTime.format(stack, modifiers, start);
        String endText   = EngineValueDateTime.format(stack, modifiers, end);

        return String.format("[%s / %s]", startText, endText);
    }
}
