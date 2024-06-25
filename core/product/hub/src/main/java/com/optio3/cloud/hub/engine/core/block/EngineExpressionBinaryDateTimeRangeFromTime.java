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
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTimeRange;
import com.optio3.cloud.hub.engine.core.value.EngineValueDuration;

@JsonTypeName("EngineExpressionBinaryDateTimeRangeFromTime")
public class EngineExpressionBinaryDateTimeRangeFromTime extends EngineOperatorBinaryFromCore<EngineValueDateTimeRange, EngineValueDateTime, EngineValueDuration>
{
    public EngineExpressionBinaryDateTimeRangeFromTime()
    {
        super(EngineValueDateTimeRange.class);
    }

    @Override
    protected EngineValueDateTimeRange computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     EngineValueDateTime time,
                                                     EngineValueDuration duration)
    {
        stack.checkNonNullValue(time, "No Time");
        stack.checkNonNullValue(duration, "No Duration");

        ZonedDateTime end   = time.value;
        ZonedDateTime start = end.minus(duration.asDuration());

        return EngineValueDateTimeRange.create(start, end);
    }
}
