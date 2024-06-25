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
import com.optio3.cloud.hub.engine.core.value.EngineValueDuration;
import com.optio3.cloud.hub.model.shared.program.CommonEngineDateTimeOperation;

@JsonTypeName("EngineExpressionBinaryDateTimeModify")
public class EngineExpressionBinaryDateTimeModify extends EngineOperatorBinaryFromCore<EngineValueDateTime, EngineValueDateTime, EngineValueDuration>
{
    public CommonEngineDateTimeOperation operation;

    public EngineExpressionBinaryDateTimeModify()
    {
        super(EngineValueDateTime.class);
    }

    @Override
    protected EngineValueDateTime computeResult(EngineExecutionContext<?, ?> ctx,
                                                EngineExecutionStack stack,
                                                EngineValueDateTime dateTime,
                                                EngineValueDuration duration)
    {
        stack.checkNonNullValue(dateTime, "No DateTime");
        stack.checkNonNullValue(duration, "No Duration");

        switch (operation)
        {
            case Add:
                return EngineValueDateTime.create(dateTime.value.plus(duration.amount, duration.unit));

            case Subtract:
                return EngineValueDateTime.create(dateTime.value.minus(duration.amount, duration.unit));

            default:
                throw stack.unexpected();
        }
    }
}
