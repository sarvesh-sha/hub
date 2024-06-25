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
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueTicket;

@JsonTypeName("AlertEngineExpressionActionNewTicket")
public class AlertEngineExpressionActionNewTicket extends AlertEngineExpressionAction<AlertEngineValueTicket>
{
    public AlertEngineExpressionActionNewTicket()
    {
        super(AlertEngineValueTicket.class);
    }

    @Override
    protected AlertEngineValueTicket computeResult(EngineExecutionContext<?, ?> ctx,
                                                   EngineExecutionStack stack,
                                                   AlertEngineValueAlert alert,
                                                   AlertEngineValueDeliveryOptions deliveryOptions)
    {
        AlertEngineValueTicket res = new AlertEngineValueTicket();
        res.alert           = alert;
        res.deliveryOptions = deliveryOptions;
        return res;
    }
}
