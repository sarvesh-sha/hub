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
import com.optio3.cloud.hub.model.tags.TagsConditionUnaryNot;

@JsonTypeName("AlertEngineOperatorUnaryAssetQueryNot")
public class AlertEngineOperatorUnaryAssetQueryNot extends EngineOperatorUnaryFromAlerts<AlertEngineValueAssetQueryCondition, AlertEngineValueAssetQueryCondition>
{
    public AlertEngineOperatorUnaryAssetQueryNot()
    {
        super(AlertEngineValueAssetQueryCondition.class);
    }

    @Override
    protected AlertEngineValueAssetQueryCondition computeResult(EngineExecutionContext<?, ?> ctx,
                                                                EngineExecutionStack stack,
                                                                AlertEngineValueAssetQueryCondition sub)
    {
        stack.checkNonNullValue(sub, "No sub query");

        return AlertEngineValueAssetQueryCondition.create(TagsConditionUnaryNot.build(sub.condition));
    }
}
