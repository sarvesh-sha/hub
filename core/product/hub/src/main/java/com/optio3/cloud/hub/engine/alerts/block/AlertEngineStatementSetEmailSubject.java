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
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;

@JsonTypeName("AlertEngineStatementSetEmailSubject")
public class AlertEngineStatementSetEmailSubject extends EngineStatementFromAlerts
{
    public EngineExpression<AlertEngineValueEmail> email;

    public EngineExpression<EngineValuePrimitiveString> subject;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, email, AlertEngineValueEmail.class, subject, EngineValuePrimitiveString.class, (email, subjectRaw) ->
        {
            stack.checkNonNullValue(email, "No Email");

            email.subject = EngineValuePrimitiveString.extract(subjectRaw);

            ctx.popBlock();
        });
    }
}
