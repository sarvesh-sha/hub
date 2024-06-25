/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPoint;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueSamples;
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTimeRange;

@JsonTypeName("AlertEngineExpressionBinaryControlPointSampleRange")
public class AlertEngineExpressionBinaryControlPointSampleRange extends EngineOperatorBinaryFromAlerts<AlertEngineValueSamples, AlertEngineValueControlPoint, EngineValueDateTimeRange>
{
    public AlertEngineExpressionBinaryControlPointSampleRange()
    {
        super(AlertEngineValueSamples.class);
    }

    @Override
    protected AlertEngineValueSamples computeResult(EngineExecutionContext<?, ?> ctx,
                                                    EngineExecutionStack stack,
                                                    AlertEngineValueControlPoint controlPoint,
                                                    EngineValueDateTimeRange range)
    {
        stack.checkNonNullValue(controlPoint, "No Control Point");
        stack.checkNonNullValue(range, "No Time Range");

        AlertEngineExecutionContext                 ctx2  = (AlertEngineExecutionContext) ctx;
        AlertEngineExecutionContext.SamplesSnapshot cache = ctx2.getSamples(controlPoint.record);

        AlertEngineValueSamples res = new AlertEngineValueSamples();
        res.controlPoint = controlPoint.record;
        res.timestamps   = cache.getTimestamps(range.start, range.end, Duration.of(2, ChronoUnit.SECONDS));

        return res;
    }
}
