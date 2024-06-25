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
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueDeliveryOptions;
import com.optio3.cloud.hub.model.DeliveryOptions;

@JsonTypeName("AlertEngineOperatorUnaryGetEmailDeliveryOptionsFromLocation")
public class AlertEngineOperatorUnaryGetEmailDeliveryOptionsFromLocation extends EngineOperatorUnaryFromAlerts<AlertEngineValueDeliveryOptions, AlertEngineValueControlPoint>
{
    public AlertEngineOperatorUnaryGetEmailDeliveryOptionsFromLocation()
    {
        super(AlertEngineValueDeliveryOptions.class);
    }

    @Override
    protected AlertEngineValueDeliveryOptions computeResult(EngineExecutionContext<?, ?> ctx,
                                                            EngineExecutionStack stack,
                                                            AlertEngineValueControlPoint controlPoint)
    {
        stack.checkNonNullValue(controlPoint, "No Control Point");

        AlertEngineExecutionContext ctx2    = (AlertEngineExecutionContext) ctx;
        DeliveryOptions             options = ctx2.getDeliveryOptionsForEmail(controlPoint.record);
        return AlertEngineValueDeliveryOptions.create(ctx, options);
    }
}
