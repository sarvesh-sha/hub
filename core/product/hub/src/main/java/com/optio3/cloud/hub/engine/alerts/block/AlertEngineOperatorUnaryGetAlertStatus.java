/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlertStatus;

@JsonTypeName("AlertEngineOperatorUnaryGetAlertStatus")
public class AlertEngineOperatorUnaryGetAlertStatus extends EngineOperatorUnaryFromAlerts<AlertEngineValueAlertStatus, AlertEngineValueAlert>
{
    public AlertEngineOperatorUnaryGetAlertStatus()
    {
        super(AlertEngineValueAlertStatus.class);
    }

    @Override
    protected AlertEngineValueAlertStatus computeResult(EngineExecutionContext<?, ?> ctx,
                                                        EngineExecutionStack stack,
                                                        AlertEngineValueAlert alert)
    {
        return alert == null ? null : AlertEngineValueAlertStatus.create(alert.status);
    }
}
