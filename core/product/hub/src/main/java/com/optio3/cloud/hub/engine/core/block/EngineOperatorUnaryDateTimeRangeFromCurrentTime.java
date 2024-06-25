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
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTimeRange;
import com.optio3.cloud.hub.engine.core.value.EngineValueDuration;

@JsonTypeName("EngineOperatorUnaryDateTimeRangeFromCurrentTime")
public class EngineOperatorUnaryDateTimeRangeFromCurrentTime extends EngineOperatorUnaryFromCore<EngineValueDateTimeRange, EngineValueDuration>
{
    public EngineOperatorUnaryDateTimeRangeFromCurrentTime()
    {
        super(EngineValueDateTimeRange.class);
    }

    @Override
    protected EngineValueDateTimeRange computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     EngineValueDuration duration)
    {
        stack.checkNonNullValue(duration, "No Duration");

        ZonedDateTime end   = ctx.thresholdTimestamp;
        ZonedDateTime start = end.minus(duration.asDuration());

        return EngineValueDateTimeRange.create(start, end);
    }
}
