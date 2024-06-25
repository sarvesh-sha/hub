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
import com.optio3.cloud.hub.model.tags.TagsConditionTerm;

@JsonTypeName("AlertEngineLiteralAssetQueryTag")
public class AlertEngineLiteralAssetQueryTag extends EngineLiteralFromAlerts<AlertEngineValueAssetQueryCondition>
{
    public String tag;

    //--//

    public AlertEngineLiteralAssetQueryTag()
    {
        super(AlertEngineValueAssetQueryCondition.class);
    }

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(AlertEngineValueAssetQueryCondition.create(TagsConditionTerm.build(tag)));
    }
}
