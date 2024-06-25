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
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAssetQueryCondition;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAssets;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.model.RecordIdentity;

@JsonTypeName("AlertEngineOperatorBinaryAssetQueryRelationSingle")
public class AlertEngineOperatorBinaryAssetQueryRelationSingle extends EngineOperatorBinaryFromAlerts<AlertEngineValueAsset, AlertEngineValueAsset, AlertEngineValueAssetQueryCondition>
{
    public AssetRelationship relation;
    public boolean           fromChild;

    //--//

    public AlertEngineOperatorBinaryAssetQueryRelationSingle()
    {
        super(AlertEngineValueAsset.class);
    }

    @Override
    protected AlertEngineValueAsset computeResult(EngineExecutionContext<?, ?> ctx,
                                                  EngineExecutionStack stack,
                                                  AlertEngineValueAsset asset,
                                                  AlertEngineValueAssetQueryCondition query)
    {
        stack.checkNonNullValue(query, "No Asset Query");

        AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

        AlertEngineValueAssets res = new AlertEngineValueAssets();

        if (asset != null && RecordIdentity.isValid(asset.record))
        {
            TagsEngine                   tagsEngine   = ctx2.getServiceNonNull(TagsEngine.class);
            TagsEngine.Snapshot          tagsSnapshot = tagsEngine.acquireSnapshot(false);
            TagsEngine.Snapshot.AssetSet results1     = tagsSnapshot.resolveRelations(asset.record.sysId, relation, fromChild);
            TagsEngine.Snapshot.AssetSet results2     = tagsSnapshot.evaluateCondition(query.condition);
            TagsEngine.Snapshot.AssetSet results      = results1.intersection(results2);

            res.addAll(ctx2, results);
        }

        return res.elements.size() == 1 ? res.elements.get(0) : null;
    }
}
