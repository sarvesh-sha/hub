/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlertSeverity;
import com.optio3.cloud.hub.model.alert.AlertSeverity;

@JsonTypeName("AlertEngineLiteralAlertSeverity")
public class AlertEngineLiteralAlertSeverity extends EngineLiteralFromAlerts<AlertEngineValueAlertSeverity>
{
    public AlertSeverity value;

    //--//

    public AlertEngineLiteralAlertSeverity()
    {
        super(AlertEngineValueAlertSeverity.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(AlertEngineValueAlertSeverity.create(value));
    }
}
