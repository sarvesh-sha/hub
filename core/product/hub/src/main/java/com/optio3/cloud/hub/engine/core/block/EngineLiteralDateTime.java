/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTime;
import com.optio3.cloud.hub.model.visualization.RangeSelection;

@JsonTypeName("EngineLiteralDateTime")
public class EngineLiteralDateTime extends EngineLiteralFromCore<EngineValueDateTime>
{
    public ZonedDateTime value;
    public String        zoneCreated;

    //--//

    public EngineLiteralDateTime()
    {
        super(EngineValueDateTime.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(EngineValueDateTime.create(RangeSelection.resolveTimeWithZone(value, null, zoneCreated)));
    }
}
