/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueSample;
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTime;

@JsonTypeName("AlertEngineOperatorUnarySampleGetTime")
public class AlertEngineOperatorUnarySampleGetTime extends EngineOperatorUnaryFromAlerts<EngineValueDateTime, AlertEngineValueSample>
{
    public AlertEngineOperatorUnarySampleGetTime()
    {
        super(EngineValueDateTime.class);
    }

    @Override
    protected EngineValueDateTime computeResult(EngineExecutionContext<?, ?> ctx,
                                                EngineExecutionStack stack,
                                                AlertEngineValueSample sample)
    {
        AlertEngineValueSample raw = stack.getNonNullValue(sample, AlertEngineValueSample.class, "null sample");

        return EngineValueDateTime.create(raw.timestamp);
    }
}
