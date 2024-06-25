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
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPoint;
import com.optio3.serialization.Reflection;

@JsonTypeName("AlertEngineOperatorUnaryAsControlPoint")
public class AlertEngineOperatorUnaryAsControlPoint extends EngineOperatorUnaryFromAlerts<AlertEngineValueControlPoint, AlertEngineValueAsset<?>>
{
    public AlertEngineOperatorUnaryAsControlPoint()
    {
        super(AlertEngineValueControlPoint.class);
    }

    @Override
    protected AlertEngineValueControlPoint computeResult(EngineExecutionContext<?, ?> ctx,
                                                         EngineExecutionStack stack,
                                                         AlertEngineValueAsset<?> asset)
    {
        return Reflection.as(asset, AlertEngineValueControlPoint.class);
    }
}
