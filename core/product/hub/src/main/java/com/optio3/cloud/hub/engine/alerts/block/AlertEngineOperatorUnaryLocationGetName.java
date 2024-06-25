/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueLocation;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;

@JsonTypeName("AlertEngineOperatorUnaryLocationGetName")
public class AlertEngineOperatorUnaryLocationGetName extends EngineOperatorUnaryFromAlerts<EngineValuePrimitiveString, AlertEngineValueLocation>
{
    public AlertEngineOperatorUnaryLocationGetName()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    protected EngineValuePrimitiveString computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       AlertEngineValueLocation location)
    {
        AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

        if (location != null)
        {
            String name = ctx2.getLocationName(location.record);
            if (name != null)
            {
                return EngineValuePrimitiveString.create(name);
            }
        }

        return null;
    }
}
