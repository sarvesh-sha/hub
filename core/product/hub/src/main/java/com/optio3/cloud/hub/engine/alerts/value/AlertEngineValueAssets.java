/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.logic.tags.TagsStreamNextAction;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;

@JsonTypeName("AlertEngineValueAssets")
public class AlertEngineValueAssets extends AlertEngineValueAbstractAssets<AssetRecord>
{
    public void addAll(AlertEngineExecutionContext ctx,
                       TagsEngine.Snapshot.AssetSet results)
    {
        if (results != null)
        {
            results.streamResolved((ri) ->
                                   {
                                       elements.add(AlertEngineValueAsset.create(ctx, ri));
                                       return TagsStreamNextAction.Continue;
                                   });
        }
    }
}
