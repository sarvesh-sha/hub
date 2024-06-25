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
import com.optio3.cloud.hub.model.tags.TagsConditionBinaryLogic;
import com.optio3.cloud.hub.model.tags.TagsConditionOperator;

@JsonTypeName("AlertEngineOperatorBinaryAssetQueryOr")
public class AlertEngineOperatorBinaryAssetQueryOr extends EngineOperatorBinaryFromAlerts<AlertEngineValueAssetQueryCondition, AlertEngineValueAssetQueryCondition, AlertEngineValueAssetQueryCondition>
{
    public AlertEngineOperatorBinaryAssetQueryOr()
    {
        super(AlertEngineValueAssetQueryCondition.class);
    }

    @Override
    protected AlertEngineValueAssetQueryCondition computeResult(EngineExecutionContext<?, ?> ctx,
                                                                EngineExecutionStack stack,
                                                                AlertEngineValueAssetQueryCondition left,
                                                                AlertEngineValueAssetQueryCondition right)
    {
        stack.checkNonNullValue(left, "No left query");
        stack.checkNonNullValue(right, "No right query");

        return AlertEngineValueAssetQueryCondition.create(TagsConditionBinaryLogic.build(left.condition, right.condition, TagsConditionOperator.Or));
    }
}
