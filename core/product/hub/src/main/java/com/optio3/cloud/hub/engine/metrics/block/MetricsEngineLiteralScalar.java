/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueScalar;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@JsonTypeName("MetricsEngineLiteralScalar")
public class MetricsEngineLiteralScalar extends EngineLiteralFromMetrics<MetricsEngineValueScalar>
{
    public double                  value;
    public EngineeringUnitsFactors units;

    //--//

    public MetricsEngineLiteralScalar()
    {
        super(MetricsEngineValueScalar.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(MetricsEngineValueScalar.create(value, units));
    }
}
