/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.model.tags.TagsCondition;

@JsonTypeName("AlertEngineValueAssetQueryCondition")
public class AlertEngineValueAssetQueryCondition extends EngineValue
{
    public TagsCondition condition;

    //--//

    public static AlertEngineValueAssetQueryCondition create(TagsCondition condition)
    {
        if (condition == null)
        {
            return null;
        }

        AlertEngineValueAssetQueryCondition res = new AlertEngineValueAssetQueryCondition();
        res.condition = condition;
        return res;
    }

    public static TagsCondition extract(AlertEngineValueAssetQueryCondition val)
    {
        return val != null ? val.condition : null;
    }

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return null;
    }
}
