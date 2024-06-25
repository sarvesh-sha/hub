/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlertStatus;
import com.optio3.cloud.hub.model.alert.AlertStatus;

@JsonTypeName("AlertEngineLiteralAlertStatus")
public class AlertEngineLiteralAlertStatus extends EngineLiteralFromAlerts<AlertEngineValueAlertStatus>
{
    public AlertStatus value;

    //--//

    public AlertEngineLiteralAlertStatus()
    {
        super(AlertEngineValueAlertStatus.class);
    }

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(AlertEngineValueAlertStatus.create(value));
    }
}
