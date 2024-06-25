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
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStepSetAlertText;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("AlertEngineStatementSetAlertText")
public class AlertEngineStatementSetAlertText extends EngineStatementFromAlerts
{
    public EngineExpression<AlertEngineValueAlert> alert;

    public EngineExpression<EngineValuePrimitiveString> text;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, alert, AlertEngineValueAlert.class, text, EngineValuePrimitiveString.class, (alert, textValue) ->
        {
            String text = EngineValuePrimitiveString.extract(textValue);

            if (alert != null && !StringUtils.equals(alert.statusText, text))
            {
                alert.shouldNotify = true;
                alert.statusText   = text;

                AlertEngineExecutionStepSetAlertText step = new AlertEngineExecutionStepSetAlertText();
                step.timestamp = alert.timestamp;
                step.record    = alert.record;
                step.text      = text;

                alert.linkStep((AlertEngineExecutionContext) ctx, step);
            }

            ctx.popBlock();
        });
    }
}
