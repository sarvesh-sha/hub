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
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStepSetAlertStatus;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlertStatus;
import com.optio3.cloud.hub.model.alert.AlertStatus;

@JsonTypeName("AlertEngineStatementSetAlertStatus")
public class AlertEngineStatementSetAlertStatus extends EngineStatementFromAlerts
{
    public EngineExpression<AlertEngineValueAlert> alert;

    public EngineExpression<AlertEngineValueAlertStatus> status;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, alert, AlertEngineValueAlert.class, status, AlertEngineValueAlertStatus.class, (alert, statusValue) ->
        {
            AlertStatus status = AlertEngineValueAlertStatus.extract(statusValue);
            if (alert != null && alert.status != status)
            {
                alert.shouldNotify = true;
                alert.status       = status;

                AlertEngineExecutionStepSetAlertStatus step = new AlertEngineExecutionStepSetAlertStatus();
                step.timestamp  = alert.timestamp;
                step.record     = alert.record;
                step.status     = status;
                step.statusText = alert.statusText;

                alert.linkStep((AlertEngineExecutionContext) ctx, step);
            }

            ctx.popBlock();
        });
    }
}
