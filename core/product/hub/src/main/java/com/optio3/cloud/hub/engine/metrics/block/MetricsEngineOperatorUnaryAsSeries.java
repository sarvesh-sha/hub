/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.serialization.Reflection;

@JsonTypeName("MetricsEngineOperatorUnaryAsSeries")
public class MetricsEngineOperatorUnaryAsSeries extends EngineOperatorUnaryFromMetrics<MetricsEngineValueSeries, EngineValue>
{
    public MetricsEngineOperatorUnaryAsSeries()
    {
        super(MetricsEngineValueSeries.class);
    }

    @Override
    protected MetricsEngineValueSeries computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     EngineValue value)
    {
        return Reflection.as(value, MetricsEngineValueSeries.class);
    }
}
