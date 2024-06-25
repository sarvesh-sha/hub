/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;

@JsonSubTypes({ @JsonSubTypes.Type(value = AlertEngineValueEmail.class), @JsonSubTypes.Type(value = AlertEngineValueSms.class), @JsonSubTypes.Type(value = AlertEngineValueTicket.class) })
public abstract class AlertEngineValueAction extends EngineValue
{
    public AlertEngineValueAlert           alert;
    public AlertEngineValueDeliveryOptions deliveryOptions;

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return null;
    }

    //--//

    public abstract void commit(AlertEngineExecutionContext ctx,
                                AlertRecord rec_alert);
}
