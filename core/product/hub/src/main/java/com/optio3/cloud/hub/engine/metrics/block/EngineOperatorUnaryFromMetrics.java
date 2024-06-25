/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.block.EngineOperatorUnary;

@JsonSubTypes({ @JsonSubTypes.Type(value = MetricsEngineOperatorAggregate.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorUnaryAsSeries.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorUnaryNot.class),
                @JsonSubTypes.Type(value = MetricsEngineOperatorUnarySelectValue.class) })
public abstract class EngineOperatorUnaryFromMetrics<To extends EngineValue, Ti extends EngineValue> extends EngineOperatorUnary<To, Ti>
{
    protected EngineOperatorUnaryFromMetrics(Class<To> resultType)
    {
        super(resultType);
    }
}
