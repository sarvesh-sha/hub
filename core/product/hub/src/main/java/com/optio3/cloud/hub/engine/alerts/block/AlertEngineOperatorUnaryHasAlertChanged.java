/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveBoolean;

@JsonTypeName("AlertEngineOperatorUnaryHasAlertChanged")
public class AlertEngineOperatorUnaryHasAlertChanged extends EngineOperatorUnaryFromAlerts<EngineValuePrimitiveBoolean, AlertEngineValueAlert>
{
    public AlertEngineOperatorUnaryHasAlertChanged()
    {
        super(EngineValuePrimitiveBoolean.class);
    }

    @Override
    protected EngineValuePrimitiveBoolean computeResult(EngineExecutionContext<?, ?> ctx,
                                                        EngineExecutionStack stack,
                                                        AlertEngineValueAlert alert)
    {
        return EngineValuePrimitiveBoolean.create(alert != null && alert.shouldNotify);
    }
}
