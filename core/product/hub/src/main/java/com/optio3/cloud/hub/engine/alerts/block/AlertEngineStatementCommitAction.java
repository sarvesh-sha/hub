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
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStepCommitAction;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAction;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;

@JsonTypeName("AlertEngineStatementCommitAction")
public class AlertEngineStatementCommitAction extends EngineStatementFromAlerts
{
    public EngineExpression<AlertEngineValueAction> action;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, action, AlertEngineValueAction.class, (action) ->
        {
            AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

            AlertEngineValueAlert alert = action.alert;
            if (alert != null)
            {
                if (alert.shouldNotify && !alert.steps.isEmpty())
                {
                    AlertEngineExecutionStepCommitAction step = new AlertEngineExecutionStepCommitAction();
                    step.details = action;
                    ctx2.pushStep(step);
                }

                //
                // Since we are committing an action, flag the alert as current, to avoid generating the same events multiple times.
                //
                AlertEngineValueAlert alertLive = ctx2.alertHolder.getAlert(alert.controlPoint, alert.type);
                if (alertLive != null)
                {
                    alertLive.shouldNotify = false;
                    alertLive.steps.clear();
                }
            }

            ctx.popBlock();
        });
    }
}
