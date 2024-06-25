/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.alerts.block.EngineLiteralFromAlerts;
import com.optio3.cloud.hub.engine.metrics.block.EngineLiteralFromMetrics;
import com.optio3.cloud.hub.engine.normalizations.block.EngineLiteralFromNormalization;

@JsonSubTypes({ @JsonSubTypes.Type(value = EngineLiteralFromCore.class),
                @JsonSubTypes.Type(value = EngineLiteralFromAlerts.class),
                @JsonSubTypes.Type(value = EngineLiteralFromMetrics.class),
                @JsonSubTypes.Type(value = EngineLiteralFromNormalization.class) })
public abstract class EngineLiteral<T extends EngineValue> extends EngineExpressionFromCore<T>
{
    protected EngineLiteral(Class<T> resultType)
    {
        super(resultType);
    }

    protected EngineLiteral(TypeReference<T> resultType)
    {
        super(resultType);
    }
}
