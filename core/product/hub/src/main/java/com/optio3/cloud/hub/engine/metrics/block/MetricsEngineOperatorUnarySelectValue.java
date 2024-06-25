/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineSelectValue;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;

@JsonTypeName("MetricsEngineOperatorUnarySelectValue")
public class MetricsEngineOperatorUnarySelectValue extends EngineOperatorUnaryFromMetrics<MetricsEngineSelectValue, MetricsEngineValueSeries>
{
    public String identifier;

    public MetricsEngineOperatorUnarySelectValue()
    {
        super(MetricsEngineSelectValue.class);
    }

    @Override
    protected MetricsEngineSelectValue computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     MetricsEngineValueSeries value)
    {
        return MetricsEngineSelectValue.create(identifier, value);
    }
}
