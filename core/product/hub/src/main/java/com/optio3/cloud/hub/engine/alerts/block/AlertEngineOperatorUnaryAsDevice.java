/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAsset;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueDevice;
import com.optio3.serialization.Reflection;

@JsonTypeName("AlertEngineOperatorUnaryAsDevice")
public class AlertEngineOperatorUnaryAsDevice extends EngineOperatorUnaryFromAlerts<AlertEngineValueDevice, AlertEngineValueAsset<?>>
{
    public AlertEngineOperatorUnaryAsDevice()
    {
        super(AlertEngineValueDevice.class);
    }

    @Override
    protected AlertEngineValueDevice computeResult(EngineExecutionContext<?, ?> ctx,
                                                   EngineExecutionStack stack,
                                                   AlertEngineValueAsset<?> asset)
    {
        return Reflection.as(asset, AlertEngineValueDevice.class);
    }
}
