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
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPoint;
import com.optio3.cloud.hub.model.alert.AlertType;

@JsonTypeName("AlertEngineOperatorUnaryGetAlert")
public class AlertEngineOperatorUnaryGetAlert extends EngineOperatorUnaryFromAlerts<AlertEngineValueAlert, AlertEngineValueControlPoint>
{
    public AlertType type;

    //--//

    public AlertEngineOperatorUnaryGetAlert()
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

        AlertEngineExecutionContext ctx2  = (AlertEngineExecutionContext) ctx;
        AlertEngineValueAlert       alert = ctx2.alertHolder.getAlert(controlPoint.record, type);

        if (alert != null)
        {
            alert.unlinkPreviousSteps();
        }

        return alert;
    }
}
