/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.metrics.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueWeeklySchedule;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;

@JsonTypeName("MetricsEngineOperatorFilterOutsideSchedule")
public class MetricsEngineOperatorFilterOutsideSchedule extends MetricsEngineOperatorBaseWithSchedule
{
    @Override
    protected MetricsEngineValueSeries computeResult(EngineExecutionContext<?, ?> ctx,
                                                     EngineExecutionStack stack,
                                                     MetricsEngineValueSeries series,
                                                     EngineValueWeeklySchedule schedule)
    {
        return computeResult(ctx, stack, series, schedule, false);
    }
}

