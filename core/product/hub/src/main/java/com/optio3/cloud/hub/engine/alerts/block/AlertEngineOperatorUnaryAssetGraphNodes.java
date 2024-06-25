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
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAssets;

@JsonTypeName("AlertEngineOperatorUnaryAssetGraphNodes")
public class AlertEngineOperatorUnaryAssetGraphNodes extends EngineExpressionFromAlerts<AlertEngineValueAssets>
{
    public String nodeId;

    //--//

    public AlertEngineOperatorUnaryAssetGraphNodes()
    {
        super(AlertEngineValueAssets.class);
    }

    @Override
    public final void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                       EngineExecutionStack stack) throws
                                                                   Exception
    {
        AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

        ctx.popBlock(ctx2.resolveGraphNodes(nodeId));
    }
}
