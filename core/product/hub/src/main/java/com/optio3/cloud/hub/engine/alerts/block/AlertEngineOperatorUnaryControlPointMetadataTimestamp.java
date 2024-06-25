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
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTime;

@JsonTypeName("AlertEngineOperatorUnaryControlPointMetadataTimestamp")
public class AlertEngineOperatorUnaryControlPointMetadataTimestamp extends EngineOperatorUnaryFromAlerts<EngineValueDateTime, AlertEngineValueControlPoint>
{
    public String key;

    public AlertEngineOperatorUnaryControlPointMetadataTimestamp()
    {
        super(EngineValueDateTime.class);
    }

    @Override
    protected EngineValueDateTime computeResult(EngineExecutionContext<?, ?> ctx,
                                                EngineExecutionStack stack,
                                                AlertEngineValueControlPoint controlPoint)
    {
        stack.checkNonNullValue(controlPoint, "No Control Point");

        AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

        return EngineValueDateTime.create(ctx2.getMetadataTimestamp(controlPoint.record, key));
    }
}
