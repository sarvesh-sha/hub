/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineInputParameter;
import com.optio3.cloud.hub.engine.EngineValue;

@JsonSubTypes({ @JsonSubTypes.Type(value = MetricsEngineInputParameterScalar.class),
                @JsonSubTypes.Type(value = MetricsEngineInputParameterSeries.class),
                @JsonSubTypes.Type(value = MetricsEngineInputParameterSeriesWithTimeOffset.class),
                @JsonSubTypes.Type(value = MetricsEngineInputParameterSetOfSeries.class) })
public abstract class EngineInputParameterFromMetrics<T extends EngineValue> extends EngineInputParameter<T>
{
    protected EngineInputParameterFromMetrics(Class<T> resultType)
    {
        super(resultType);
    }
}
