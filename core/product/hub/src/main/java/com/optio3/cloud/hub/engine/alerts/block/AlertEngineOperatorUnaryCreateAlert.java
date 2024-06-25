/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStepCreateAlert;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStepSetAlertStatus;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPoint;
import com.optio3.cloud.hub.model.alert.AlertSeverity;
import com.optio3.cloud.hub.model.alert.AlertStatus;
import com.optio3.cloud.hub.model.alert.AlertType;

@JsonTypeName("AlertEngineOperatorUnaryCreateAlert")
public class AlertEngineOperatorUnaryCreateAlert extends EngineOperatorUnaryFromAlerts<AlertEngineValueAlert, AlertEngineValueControlPoint>
{
    public AlertType     type;
    public AlertSeverity severity;

    //--//

    public AlertEngineOperatorUnaryCreateAlert()
    {
        super(AlertEngineValueAlert.class);
    }

    @Override
    protected AlertEngineValueAlert computeResult(EngineExecutionContext<?, ?> ctx,
                                                  EngineExecutionStack stack,
                                                  AlertEngineValueControlPoint controlPoint) throws
                                                                                             Exception
    {
        stack.checkNonNullValue(controlPoint, "No Control Point");

        AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

        AlertEngineValueAlert alert = ctx2.alertHolder.getAlert(controlPoint.record, type);
        if (alert != null)
        {
            if (severity != null && alert.severity != severity)
            {
                alert.shouldNotify = true;
            }

            switch (alert.status)
            {
                case closed:
                    // Create a new alert.
                    alert = null;
                    break;

                case resolved:
                    // Reactivate existing alert.
                    alert.shouldNotify = true;
                    alert.status = AlertStatus.active;
                    alert.unlinkPreviousSteps();

                    AlertEngineExecutionStepSetAlertStatus step = new AlertEngineExecutionStepSetAlertStatus();
                    step.record = alert.record;
                    step.status = AlertStatus.active;
                    step.statusText = alert.statusText;

                    alert.linkStep(ctx2, step);
                    break;
            }
        }

        if (alert == null)
        {
            alert = ctx2.alertHolder.createAlert(controlPoint.record, type);
            alert.unlinkPreviousSteps();

            AlertEngineExecutionStepCreateAlert step = new AlertEngineExecutionStepCreateAlert();
            step.controlPoint = alert.controlPoint;
            step.record       = alert.record;
            step.type         = type;
            step.severity     = severity;

            alert.linkStep(ctx2, step);
        }

        return alert;
    }
}
