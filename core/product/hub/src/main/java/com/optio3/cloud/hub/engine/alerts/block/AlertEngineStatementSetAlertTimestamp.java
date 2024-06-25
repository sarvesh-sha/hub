/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStep;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueSample;
import com.optio3.cloud.hub.engine.core.value.EngineValueDateTime;

@JsonTypeName("AlertEngineStatementSetAlertTimestamp")
public class AlertEngineStatementSetAlertTimestamp extends EngineStatementFromAlerts
{
    public EngineExpression<AlertEngineValueAlert> alert;

    public EngineExpression<AlertEngineValueSample> sample;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, alert, AlertEngineValueAlert.class, sample, EngineValue.class, (alert, sample) ->
        {
            if (alert != null && alert.shouldNotify)
            {
                ZonedDateTime timestamp;

                if (sample instanceof AlertEngineValueSample)
                {
                    timestamp = ((AlertEngineValueSample) sample).timestamp;
                }
                else if (sample instanceof EngineValueDateTime)
                {
                    timestamp = ((EngineValueDateTime) sample).value;
                }
                else
                {
                    timestamp = null;
                }

                if (timestamp != null)
                {
                    alert.timestamp = timestamp;

                    //
                    // Because this action requires an alert, we need to backpatch the steps that allocated the alert.
                    //
                    for (AlertEngineExecutionStep step : alert.steps)
                    {
                        step.timestamp = timestamp;
                    }
                }
            }

            ctx.popBlock();
        });
    }
}
