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
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTime;

@JsonTypeName("AlertEngineExpressionBinaryControlPointSample")
public class AlertEngineExpressionBinaryControlPointSample extends EngineOperatorBinaryFromAlerts<AlertEngineValueSample, AlertEngineValueControlPoint, EngineValueDateTime>
{
    public AlertEngineExpressionBinaryControlPointSample()
    {
        super(AlertEngineValueSample.class);
    }

    @Override
    protected AlertEngineValueSample computeResult(EngineExecutionContext<?, ?> ctx,
                                                   EngineExecutionStack stack,
                                                   AlertEngineValueControlPoint controlPoint,
                                                   EngineValueDateTime timestamp)
    {
        stack.checkNonNullValue(controlPoint, "No Control Point");

        AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;
        ctx2.markControlPointAsSeen(controlPoint.record);

        return timestamp != null ? AlertEngineValueSample.create(controlPoint.record, timestamp.value) : null;
    }
}
