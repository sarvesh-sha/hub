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
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueSms;

@JsonTypeName("AlertEngineExpressionActionNewSms")
public class AlertEngineExpressionActionNewSms extends AlertEngineExpressionAction<AlertEngineValueSms>
{
    public AlertEngineExpressionActionNewSms()
    {
        super(AlertEngineValueSms.class);
    }

    @Override
    protected AlertEngineValueSms computeResult(EngineExecutionContext<?, ?> ctx,
                                                EngineExecutionStack stack,
                                                AlertEngineValueAlert alert,
                                                AlertEngineValueDeliveryOptions deliveryOptions)
    {
        AlertEngineValueSms res = new AlertEngineValueSms();
        res.alert           = alert;
        res.deliveryOptions = deliveryOptions;
        return res;
    }
}
