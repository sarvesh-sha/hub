/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.value;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.serialization.Reflection;
import com.optio3.util.TimeUtils;

@JsonTypeName("EngineValueDuration")
public class EngineValueDuration extends EngineValue
{
    public int        amount;
    public ChronoUnit unit;

    public static EngineValueDuration create(int amount,
                                             ChronoUnit unit)
    {
        if (unit == null)
        {
            return null;
        }

        EngineValueDuration res = new EngineValueDuration();
        res.amount = amount;
        res.unit   = unit;
        return res;
    }

    public Duration asDuration()
    {
        return TimeUtils.computeSafeDuration(amount, unit);
    }

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        EngineValueDuration other = Reflection.as(o, EngineValueDuration.class);
        if (other != null)
        {
            return this.asDuration()
                       .compareTo(other.asDuration());
        }

        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return String.format("%d %s", amount, unit);
    }
}
