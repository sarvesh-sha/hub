/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueTravelEntry;
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTime;
import com.optio3.util.TimeUtils;

@JsonTypeName("AlertEngineOperatorUnaryTravelEntryGetTime")
public class AlertEngineOperatorUnaryTravelEntryGetTime extends EngineOperatorUnaryFromAlerts<EngineValueDateTime, AlertEngineValueTravelEntry>
{
    public AlertEngineOperatorUnaryTravelEntryGetTime()
    {
        super(EngineValueDateTime.class);
    }

    @Override
    protected EngineValueDateTime computeResult(EngineExecutionContext<?, ?> ctx,
                                                EngineExecutionStack stack,
                                                AlertEngineValueTravelEntry sample)
    {
        return sample != null ? EngineValueDateTime.create(TimeUtils.fromTimestampToUtcTime(sample.timestamp)) : null;
    }
}
