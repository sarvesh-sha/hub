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
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPointCoordinates;

@JsonTypeName("AlertEngineOperatorUnaryControlPointCoordinates")
public class AlertEngineOperatorUnaryControlPointCoordinates extends EngineOperatorUnaryFromAlerts<AlertEngineValueControlPointCoordinates, AlertEngineValueControlPoint>
{
    public AlertEngineOperatorUnaryControlPointCoordinates()
    {
        super(AlertEngineValueControlPointCoordinates.class);
    }

    @Override
    protected AlertEngineValueControlPointCoordinates computeResult(EngineExecutionContext<?, ?> ctx,
                                                                    EngineExecutionStack stack,
                                                                    AlertEngineValueControlPoint controlPoint)
    {
        stack.checkNonNullValue(controlPoint, "No Control Point");

        AlertEngineExecutionContext                     ctx2  = (AlertEngineExecutionContext) ctx;
        AlertEngineExecutionContext.CoordinatesSnapshot cache = ctx2.getCoordinates(controlPoint.record);

        return cache != null ? cache.getCoordinates() : null;
    }
}
