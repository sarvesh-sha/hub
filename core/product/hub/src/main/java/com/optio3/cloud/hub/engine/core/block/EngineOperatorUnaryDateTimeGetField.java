/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTime;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;

@JsonTypeName("EngineOperatorUnaryDateTimeGetField")
public class EngineOperatorUnaryDateTimeGetField extends EngineOperatorUnaryFromCore<EngineValuePrimitiveNumber, EngineValueDateTime>
{
    public ChronoUnit unit;

    public EngineOperatorUnaryDateTimeGetField()
    {
        super(EngineValuePrimitiveNumber.class);
    }

    @Override
    protected EngineValuePrimitiveNumber computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       EngineValueDateTime dateTime)
    {
        stack.checkNonNullValue(dateTime, "No Timestamp");

        switch (unit)
        {
            case NANOS:
                return EngineValuePrimitiveNumber.create(dateTime.value.getNano());

            case MICROS:
                return EngineValuePrimitiveNumber.create(dateTime.value.getNano() / 1_000);

            case MILLIS:
                return EngineValuePrimitiveNumber.create(dateTime.value.getNano() / 1_000_000);

            case SECONDS:
                return EngineValuePrimitiveNumber.create(dateTime.value.getSecond());

            case MINUTES:
                return EngineValuePrimitiveNumber.create(dateTime.value.getMinute());

            case HOURS:
                return EngineValuePrimitiveNumber.create(dateTime.value.getHour());

            case DAYS:
                return EngineValuePrimitiveNumber.create(dateTime.value.getDayOfMonth());

            case MONTHS:
                return EngineValuePrimitiveNumber.create(dateTime.value.getMonthValue());

            case YEARS:
                return EngineValuePrimitiveNumber.create(dateTime.value.getYear());

            default:
                throw stack.unexpected();
        }
    }
}
