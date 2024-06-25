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
import com.optio3.cloud.hub.engine.core.value.EngineValueTimeZone;

@JsonTypeName("AlertEngineOperatorUnaryGetTimeZoneFromLocation")
public class AlertEngineOperatorUnaryGetTimeZoneFromLocation extends EngineOperatorUnaryFromAlerts<EngineValueTimeZone, AlertEngineValueControlPoint>
{
    public AlertEngineOperatorUnaryGetTimeZoneFromLocation()
    {
        super(EngineValueTimeZone.class);
    }

    @Override
    protected EngineValueTimeZone computeResult(EngineExecutionContext<?, ?> ctx,
                                                EngineExecutionStack stack,
                                                AlertEngineValueControlPoint controlPoint)
    {
        stack.checkNonNullValue(controlPoint, "No Control Point");

        AlertEngineExecutionContext ctx2     = (AlertEngineExecutionContext) ctx;
        String                      timeZone = ctx2.getTimeZone(controlPoint.record);
        return EngineValueTimeZone.create(timeZone);
    }
}
