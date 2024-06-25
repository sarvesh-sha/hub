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
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStepSetAlertSeverity;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlertSeverity;
import com.optio3.cloud.hub.model.alert.AlertSeverity;

@JsonTypeName("AlertEngineStatementSetAlertSeverity")
public class AlertEngineStatementSetAlertSeverity extends EngineStatementFromAlerts
{
    public EngineExpression<AlertEngineValueAlert> alert;

    public EngineExpression<AlertEngineValueAlertSeverity> severity;

    //--//

    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, alert, AlertEngineValueAlert.class, severity, AlertEngineValueAlertSeverity.class, (alert, severityValue) ->
        {
            AlertSeverity severity = AlertEngineValueAlertSeverity.extract(severityValue);
            if (alert != null && alert.severity != severity)
            {
                alert.shouldNotify = true;
                alert.severity     = severity;

                AlertEngineExecutionStepSetAlertSeverity step = new AlertEngineExecutionStepSetAlertSeverity();
                step.timestamp = alert.timestamp;
                step.record    = alert.record;
                step.severity  = severity;

                alert.linkStep((AlertEngineExecutionContext) ctx, step);
            }

            ctx.popBlock();
        });
    }
}
