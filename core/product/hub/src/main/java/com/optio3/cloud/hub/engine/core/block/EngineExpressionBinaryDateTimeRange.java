/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTime;
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTimeRange;

@JsonTypeName("EngineExpressionBinaryDateTimeRange")
public class EngineExpressionBinaryDateTimeRange extends EngineOperatorBinaryFromCore<EngineValueDateTimeRange, EngineValueDateTime, EngineValueDateTime>
{
    public EngineExpressionBinaryDateTimeRange()
    {
        super(EngineValueDateTimeRange.class);
    }

    @Override
    protected EngineValueDateTimeRange computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     EngineValueDateTime valueStart,
                                                     EngineValueDateTime valueEnd)
    {
        stack.checkNonNullValue(valueStart, "No Range Start");
        stack.checkNonNullValue(valueEnd, "No Range End");

        return EngineValueDateTimeRange.create(valueStart.value, valueEnd.value);
    }
}
