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
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStepSetAlertDescription;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("AlertEngineStatementSetAlertDescription")
public class AlertEngineStatementSetAlertDescription extends EngineStatementFromAlerts
{
    public EngineExpression<AlertEngineValueAlert> alert;

    public EngineExpression<EngineValuePrimitiveString> description;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, alert, AlertEngineValueAlert.class, description, EngineValuePrimitiveString.class, (alert, descriptionValue) ->
        {
            String description = EngineValuePrimitiveString.extract(descriptionValue);

            if (alert != null && !StringUtils.equals(alert.description, description))
            {
                alert.description = description;

                AlertEngineExecutionStepSetAlertDescription step = new AlertEngineExecutionStepSetAlertDescription();
                step.timestamp   = alert.timestamp;
                step.record      = alert.record;
                step.description = description;

                alert.linkStep((AlertEngineExecutionContext) ctx, step);
            }

            ctx.popBlock();
        });
    }
}
