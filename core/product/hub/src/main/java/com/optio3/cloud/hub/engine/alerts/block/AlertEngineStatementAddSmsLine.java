/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueEmail;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueSms;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;

@JsonTypeName("AlertEngineStatementAddSmsLine")
public class AlertEngineStatementAddSmsLine extends EngineStatementFromAlerts
{
    public EngineExpression<AlertEngineValueSms> sms;

    public EngineExpression<EngineValuePrimitiveString> text;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, sms, AlertEngineValueSms.class, text, EngineValuePrimitiveString.class, (sms, subjectRaw) ->
        {
            stack.checkNonNullValue(sms, "No Email");

            if (sms.body == null)
            {
                sms.body = Lists.newArrayList();
            }

            sms.body.add(EngineValuePrimitiveString.extract(subjectRaw));

            ctx.popBlock();
        });
    }
}
