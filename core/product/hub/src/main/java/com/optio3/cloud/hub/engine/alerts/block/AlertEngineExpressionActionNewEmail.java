/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueDeliveryOptions;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueEmail;

@JsonTypeName("AlertEngineExpressionActionNewEmail")
public class AlertEngineExpressionActionNewEmail extends AlertEngineExpressionAction<AlertEngineValueEmail>
{
    public AlertEngineExpressionActionNewEmail()
    {
        super(AlertEngineValueEmail.class);
    }

    @Override
    protected AlertEngineValueEmail computeResult(EngineExecutionContext<?, ?> ctx,
                                                  EngineExecutionStack stack,
                                                  AlertEngineValueAlert alert,
                                                  AlertEngineValueDeliveryOptions deliveryOptions)
    {
        stack.checkNonNullValue(deliveryOptions, "No Delivery Options");

        AlertEngineValueEmail res = new AlertEngineValueEmail();
        res.alert           = alert;
        res.deliveryOptions = deliveryOptions;
        return res;
    }
}
