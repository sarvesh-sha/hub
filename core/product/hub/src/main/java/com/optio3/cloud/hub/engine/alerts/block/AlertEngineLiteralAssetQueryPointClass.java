/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAssetQueryCondition;
import com.optio3.cloud.hub.model.tags.TagsConditionTermWithValue;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;

@JsonTypeName("AlertEngineLiteralAssetQueryPointClass")
public class AlertEngineLiteralAssetQueryPointClass extends EngineLiteralFromAlerts<AlertEngineValueAssetQueryCondition>
{
    public String pointClass;

    //--//

    public AlertEngineLiteralAssetQueryPointClass()
    {
        super(AlertEngineValueAssetQueryCondition.class);
    }

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(AlertEngineValueAssetQueryCondition.create(TagsConditionTermWithValue.build(AssetRecord.WellKnownTags.pointClassId, pointClass)));
    }
}
