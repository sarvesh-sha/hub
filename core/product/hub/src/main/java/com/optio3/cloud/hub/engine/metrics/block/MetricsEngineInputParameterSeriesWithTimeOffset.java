/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.metrics.MetricsEngineExecutionContext;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;

@JsonTypeName("MetricsEngineInputParameterSeriesWithTimeOffset")
public class MetricsEngineInputParameterSeriesWithTimeOffset extends EngineInputParameterFromMetrics<MetricsEngineValueSeries>
{
    public String     nodeId;
    public int        timeShift;
    public ChronoUnit timeShiftUnit;

    //--//

    public MetricsEngineInputParameterSeriesWithTimeOffset()
    {
        super(MetricsEngineValueSeries.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        MetricsEngineExecutionContext ctx2 = (MetricsEngineExecutionContext) ctx;

        ctx2.popBlock(ctx2.getSeries(nodeId, timeShift, timeShiftUnit));
    }
}
