/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.hub.engine.alerts.block.EngineExpressionFromAlerts;
import com.optio3.cloud.hub.engine.core.block.EngineExpressionFromCore;
import com.optio3.cloud.hub.engine.metrics.block.EngineExpressionFromMetrics;
import com.optio3.cloud.hub.engine.normalizations.block.EngineExpressionFromNormalization;
import com.optio3.serialization.Reflection;

@JsonSubTypes({ @JsonSubTypes.Type(value = EngineExpressionFromCore.class),
                @JsonSubTypes.Type(value = EngineExpressionFromAlerts.class),
                @JsonSubTypes.Type(value = EngineExpressionFromMetrics.class),
                @JsonSubTypes.Type(value = EngineExpressionFromNormalization.class) })
public abstract class EngineExpression<T extends EngineValue> extends EngineBlock
{
    @JsonIgnore
    public final Class<T> resultType;

    protected EngineExpression(Class<T> resultType)
    {
        this.resultType = resultType;
    }

    protected EngineExpression(TypeReference<T> resultType)
    {
        this.resultType = Reflection.getRawType(resultType.getType());
    }

    @SuppressWarnings("unchecked")
    public static <O extends EngineValue, I extends O> EngineExpression<O> cast(EngineExpression<I> val)
    {
        return (EngineExpression<O>) val;
    }
}
