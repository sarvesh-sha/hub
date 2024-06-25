/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.EngineValue;

@JsonSubTypes({ @JsonSubTypes.Type(value = AlertEngineExpressionAction.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryAssetGraphNode.class),
                @JsonSubTypes.Type(value = AlertEngineOperatorUnaryAssetGraphNodes.class) })
public abstract class EngineExpressionFromAlerts<T extends EngineValue> extends EngineExpression<T>
{
    protected EngineExpressionFromAlerts(Class<T> resultType)
    {
        super(resultType);
    }
}
