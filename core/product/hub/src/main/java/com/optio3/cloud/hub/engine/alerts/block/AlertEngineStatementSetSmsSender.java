/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueEmail;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueSms;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;

@JsonTypeName("AlertEngineStatementSetSmsSender")
public class AlertEngineStatementSetSmsSender extends EngineStatementFromAlerts
{
    public EngineExpression<AlertEngineValueSms> sms;

    public EngineExpression<EngineValuePrimitiveString> sender;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, sms, AlertEngineValueSms.class, sender, EngineValuePrimitiveString.class, (sms, senderRaw) ->
        {
            stack.checkNonNullValue(sms, "No SMS");

            sms.sender = EngineValuePrimitiveString.extract(senderRaw);

            ctx.popBlock();
        });
    }
}
