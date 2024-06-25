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
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAssetQueryCondition;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAssets;
import com.optio3.cloud.hub.logic.tags.TagsEngine;

@JsonTypeName("AlertEngineOperatorUnaryAssetQueryExec")
public class AlertEngineOperatorUnaryAssetQueryExec extends EngineOperatorUnaryFromAlerts<AlertEngineValueAssets, AlertEngineValueAssetQueryCondition>
{
    public AlertEngineOperatorUnaryAssetQueryExec()
    {
        super(AlertEngineValueAssets.class);
    }

    @Override
    protected AlertEngineValueAssets computeResult(EngineExecutionContext<?, ?> ctx,
                                                   EngineExecutionStack stack,
                                                   AlertEngineValueAssetQueryCondition query)
    {
        stack.checkNonNullValue(query, "No Asset Query");

        AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

        TagsEngine                   tagsEngine   = ctx2.getServiceNonNull(TagsEngine.class);
        TagsEngine.Snapshot          tagsSnapshot = tagsEngine.acquireSnapshot(false);
        TagsEngine.Snapshot.AssetSet results      = tagsSnapshot.evaluateCondition(query.condition);

        AlertEngineValueAssets res = new AlertEngineValueAssets();
        res.addAll(ctx2, results);

        return res;
    }
}
