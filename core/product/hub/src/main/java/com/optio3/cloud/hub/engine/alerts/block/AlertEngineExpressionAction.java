/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueDeliveryOptions;

@JsonSubTypes({ @JsonSubTypes.Type(value = AlertEngineExpressionActionNewTicket.class),
                @JsonSubTypes.Type(value = AlertEngineExpressionActionNewEmail.class),
                @JsonSubTypes.Type(value = AlertEngineExpressionActionNewSms.class) })
public abstract class AlertEngineExpressionAction<T extends EngineValue> extends EngineExpressionFromAlerts<T>
{
    public EngineExpression<AlertEngineValueAlert> alert;

    public EngineExpression<AlertEngineValueDeliveryOptions> deliveryOptions;

    protected AlertEngineExpressionAction(Class<T> resultType)
    {
        super(resultType);
    }

    @Override
    public final void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                       EngineExecutionStack stack) throws
                                                                   Exception
    {
        extractParams(ctx, stack, alert, AlertEngineValueAlert.class, deliveryOptions, AlertEngineValueDeliveryOptions.class, (alert, deliveryOptions) ->
        {
            if (alert == null)
            {
                throw stack.unexpected("No Alert");
            }

            ctx.popBlock(computeResult(ctx, stack, alert.copy(), deliveryOptions));
        });
    }

    protected abstract T computeResult(EngineExecutionContext<?, ?> ctx,
                                       EngineExecutionStack stack,
                                       AlertEngineValueAlert alert,
                                       AlertEngineValueDeliveryOptions deliveryOptions);
}
