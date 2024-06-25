/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPoint;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueSample;

@JsonTypeName("AlertEngineOperatorUnaryControlPointLastSample")
public class AlertEngineOperatorUnaryControlPointLastSample extends EngineOperatorUnaryFromAlerts<AlertEngineValueSample, AlertEngineValueControlPoint>
{
    public AlertEngineOperatorUnaryControlPointLastSample()
    {
        super(AlertEngineValueSample.class);
    }

    @Override
    protected AlertEngineValueSample computeResult(EngineExecutionContext<?, ?> ctx,
                                                   EngineExecutionStack stack,
                                                   AlertEngineValueControlPoint controlPoint)
    {
        stack.checkNonNullValue(controlPoint, "No Control Point");

        AlertEngineExecutionContext                 ctx2  = (AlertEngineExecutionContext) ctx;
        AlertEngineExecutionContext.SamplesSnapshot cache = ctx2.getSamples(controlPoint.record);

        ctx2.markControlPointAsSeen(controlPoint.record);

        return AlertEngineValueSample.create(controlPoint.record, cache.getLastTimestamp());
    }
}
