/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.EngineValue;

@JsonSubTypes({ @JsonSubTypes.Type(value = MetricsEngineBaseSelectValue.class),
                @JsonSubTypes.Type(value = MetricsEngineCreateVector3.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorGpsDistance.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorGpsSunElevation.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorThresholdRange.class) })
public abstract class EngineExpressionFromMetrics<T extends EngineValue> extends EngineExpression<T>
{
    protected EngineExpressionFromMetrics(Class<T> resultType)
    {
        super(resultType);
    }

    protected EngineExpressionFromMetrics(TypeReference<T> resultType)
    {
        super(resultType);
    }
}
