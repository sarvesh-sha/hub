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
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;

@JsonTypeName("AlertEngineOperatorUnaryControlPointMetadataNumber")
public class AlertEngineOperatorUnaryControlPointMetadataNumber extends EngineOperatorUnaryFromAlerts<EngineValuePrimitiveNumber, AlertEngineValueControlPoint>
{
    public String key;

    public AlertEngineOperatorUnaryControlPointMetadataNumber()
    {
        super(EngineValuePrimitiveNumber.class);
    }

    @Override
    protected EngineValuePrimitiveNumber computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       AlertEngineValueControlPoint controlPoint)
    {
        stack.checkNonNullValue(controlPoint, "No Control Point");

        AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

        return EngineValuePrimitiveNumber.create(ctx2.getMetadataDouble(controlPoint.record, key));
    }
}
