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
import com.optio3.cloud.hub.engine.core.value.EngineValueTimeZone;

@JsonTypeName("EngineExpressionBinaryDateTimeSetTimeZone")
public class EngineExpressionBinaryDateTimeSetTimeZone extends EngineOperatorBinaryFromCore<EngineValueDateTime, EngineValueDateTime, EngineValueTimeZone>
{
    public EngineExpressionBinaryDateTimeSetTimeZone()
    {
        super(EngineValueDateTime.class);
    }

    @Override
    protected EngineValueDateTime computeResult(EngineExecutionContext<?, ?> ctx,
                                                EngineExecutionStack stack,
                                                EngineValueDateTime dateTime,
                                                EngineValueTimeZone timeZone)
    {
        if (dateTime == null)
        {
            return null;
        }

        ZonedDateTime result = dateTime.value;

        if (timeZone != null)
        {
            result = result.withZoneSameInstant(timeZone.resolve(stack));
        }

        return EngineValueDateTime.create(result);
    }
}
