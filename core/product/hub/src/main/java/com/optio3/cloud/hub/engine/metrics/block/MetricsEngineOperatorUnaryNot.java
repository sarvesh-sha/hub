/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.protocol.model.EngineeringUnits;

@JsonTypeName("MetricsEngineOperatorUnaryNot")
public class MetricsEngineOperatorUnaryNot extends EngineOperatorUnaryFromMetrics<MetricsEngineValueSeries, MetricsEngineValueSeries>
{
    public MetricsEngineOperatorUnaryNot()
    {
        super(MetricsEngineValueSeries.class);
    }

    @Override
    protected MetricsEngineValueSeries computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     MetricsEngineValueSeries series)
    {
        series = series.copy();
        series.values.setUnits(EngineeringUnits.activeInactive);

        double[] values = series.values.values;

        for (int i = 0; i < values.length; i++)
        {
            values[i] = (values[i] > 0.0) ? 0 : 1;
        }

        return series;
    }
}
