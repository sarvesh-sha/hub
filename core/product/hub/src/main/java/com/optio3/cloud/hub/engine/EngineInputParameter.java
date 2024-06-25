/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.alerts.block.EngineInputParameterFromAlerts;
import com.optio3.cloud.hub.engine.core.block.EngineExpressionFromCore;
import com.optio3.cloud.hub.engine.core.block.EngineInputParameterFromCore;
import com.optio3.cloud.hub.engine.metrics.block.EngineInputParameterFromMetrics;

@JsonSubTypes({ @JsonSubTypes.Type(value = EngineInputParameterFromCore.class),
                @JsonSubTypes.Type(value = EngineInputParameterFromAlerts.class),
                @JsonSubTypes.Type(value = EngineInputParameterFromMetrics.class) })
public abstract class EngineInputParameter<T extends EngineValue> extends EngineExpressionFromCore<T>
{
    protected EngineInputParameter(Class<T> resultType)
    {
        super(resultType);
    }

    public String title;

    public String description;
}
