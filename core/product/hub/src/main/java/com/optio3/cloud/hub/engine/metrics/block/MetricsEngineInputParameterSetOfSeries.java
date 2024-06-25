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
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSetOfSeries;

@JsonTypeName("MetricsEngineInputParameterSetOfSeries")
public class MetricsEngineInputParameterSetOfSeries extends EngineInputParameterFromMetrics<MetricsEngineValueSetOfSeries>
{
    public String nodeId;

    //--//

    public MetricsEngineInputParameterSetOfSeries()
    {
        super(MetricsEngineValueSetOfSeries.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        MetricsEngineExecutionContext ctx2 = (MetricsEngineExecutionContext) ctx;

        ctx2.popBlock(ctx2.getSetOfSeries(nodeId, 0, null));
    }
}
