/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.block.EngineLiteral;

@JsonSubTypes({ @JsonSubTypes.Type(value = MetricsEngineLiteralScalar.class) })
public abstract class EngineLiteralFromMetrics<T extends EngineValue> extends EngineLiteral<T>
{
    protected EngineLiteralFromMetrics(Class<T> resultType)
    {
        super(resultType);
    }
}
