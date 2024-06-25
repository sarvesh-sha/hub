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
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueLogicalAsset;
import com.optio3.serialization.Reflection;

@JsonTypeName("AlertEngineOperatorUnaryAsGroup")
public class AlertEngineOperatorUnaryAsGroup extends EngineOperatorUnaryFromAlerts<AlertEngineValueLogicalAsset, AlertEngineValueAsset<?>>
{
    public AlertEngineOperatorUnaryAsGroup()
    {
        super(AlertEngineValueLogicalAsset.class);
    }

    @Override
    protected AlertEngineValueLogicalAsset computeResult(EngineExecutionContext<?, ?> ctx,
                                                         EngineExecutionStack stack,
                                                         AlertEngineValueAsset<?> asset)
    {
        return Reflection.as(asset, AlertEngineValueLogicalAsset.class);
    }
}
