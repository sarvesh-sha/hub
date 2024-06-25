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
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;

@JsonTypeName("AlertEngineOperatorUnaryControlPointMetadataString")
public class AlertEngineOperatorUnaryControlPointMetadataString extends EngineOperatorUnaryFromAlerts<EngineValuePrimitiveString, AlertEngineValueControlPoint>
{
    public String key;

    public AlertEngineOperatorUnaryControlPointMetadataString()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    protected EngineValuePrimitiveString computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       AlertEngineValueControlPoint controlPoint)
    {
        stack.checkNonNullValue(controlPoint, "No Control Point");

        AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

        return EngineValuePrimitiveString.create(ctx2.getMetadataString(controlPoint.record, key));
    }
}
