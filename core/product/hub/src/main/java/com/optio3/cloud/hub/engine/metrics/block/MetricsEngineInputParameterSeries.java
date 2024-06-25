/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.metrics.MetricsEngineExecutionContext;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;

@JsonTypeName("MetricsEngineInputParameterSeries")
public class MetricsEngineInputParameterSeries extends EngineInputParameterFromMetrics<MetricsEngineValueSeries>
{
    public String nodeId;

    //--//

    public MetricsEngineInputParameterSeries()
    {
        super(MetricsEngineValueSeries.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        MetricsEngineExecutionContext ctx2 = (MetricsEngineExecutionContext) ctx;

        ctx2.popBlock(ctx2.getSeries(nodeId, 0, null));
    }
}
