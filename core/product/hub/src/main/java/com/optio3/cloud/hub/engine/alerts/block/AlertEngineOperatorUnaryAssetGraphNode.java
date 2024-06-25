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
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAsset;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAssetQueryCondition;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAssets;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueDeliveryOptions;
import com.optio3.cloud.hub.logic.tags.TagsEngine;

@JsonTypeName("AlertEngineOperatorUnaryAssetGraphNode")
public class AlertEngineOperatorUnaryAssetGraphNode extends EngineExpressionFromAlerts<AlertEngineValueAsset>
{
    public String nodeId;

    //--//

    public AlertEngineOperatorUnaryAssetGraphNode()
    {
        super(AlertEngineValueAsset.class);
    }

    @Override
    public final void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                       EngineExecutionStack stack) throws
                                                                   Exception
    {
        AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

        ctx.popBlock(ctx2.resolveGraphNode(nodeId));
    }
}
