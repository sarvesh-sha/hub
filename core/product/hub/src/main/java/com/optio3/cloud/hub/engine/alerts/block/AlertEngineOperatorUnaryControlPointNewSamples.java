/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPoint;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueSamples;
import com.optio3.util.CollectionUtils;

@JsonTypeName("AlertEngineOperatorUnaryControlPointNewSamples")
public class AlertEngineOperatorUnaryControlPointNewSamples extends EngineOperatorUnaryFromAlerts<AlertEngineValueSamples, AlertEngineValueControlPoint>
{
    public AlertEngineOperatorUnaryControlPointNewSamples()
    {
        super(AlertEngineValueSamples.class);
    }

    @Override
    protected AlertEngineValueSamples computeResult(EngineExecutionContext<?, ?> ctx,
                                                    EngineExecutionStack stack,
                                                    AlertEngineValueControlPoint controlPoint)
    {
        stack.checkNonNullValue(controlPoint, "No Control Point");

        AlertEngineExecutionContext                 ctx2  = (AlertEngineExecutionContext) ctx;
        AlertEngineExecutionContext.SamplesSnapshot cache = ctx2.getSamples(controlPoint.record);

        ZonedDateTime lastTimestamp = ctx2.getLastSeenSample(controlPoint.record);

        // Move one second past the last seen sample.
        lastTimestamp = lastTimestamp.plus(1, ChronoUnit.SECONDS);

        AlertEngineValueSamples res = new AlertEngineValueSamples();
        res.controlPoint = controlPoint.record;
        res.timestamps   = cache.getTimestamps(lastTimestamp, null, Duration.of(2, ChronoUnit.SECONDS));

        ZonedDateTime reportedTimestamp = CollectionUtils.lastElement(res.timestamps);
        if (reportedTimestamp != null)
        {
            ctx2.setLastSeenSample(controlPoint.record, reportedTimestamp);
        }
        else
        {
            ctx2.markControlPointAsSeen(controlPoint.record);
        }

        return res;
    }
}
