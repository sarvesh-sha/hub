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
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAsset;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;

@JsonTypeName("AlertEngineOperatorUnaryAssetGetName")
public class AlertEngineOperatorUnaryAssetGetName extends EngineOperatorUnaryFromAlerts<EngineValuePrimitiveString, AlertEngineValueAsset<?>>
{
    public AlertEngineOperatorUnaryAssetGetName()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    protected EngineValuePrimitiveString computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       AlertEngineValueAsset<?> asset)
    {
        AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

        if (asset != null)
        {
            String name = ctx2.getAssetName(asset.record);

            return EngineValuePrimitiveString.create(name);
        }

        return null;
    }
}
