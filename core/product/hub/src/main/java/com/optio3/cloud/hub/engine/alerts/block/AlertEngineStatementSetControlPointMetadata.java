/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPoint;

@JsonTypeName("AlertEngineStatementSetControlPointMetadata")
public class AlertEngineStatementSetControlPointMetadata extends EngineStatementFromAlerts
{
    public EngineExpression<AlertEngineValueControlPoint> controlPoint;

    public String key;

    public EngineExpression<EngineValue> value;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, controlPoint, AlertEngineValueControlPoint.class, value, EngineValue.class, (controlPoint, value) ->
        {
            AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

            ctx2.setMetadataValue(controlPoint.record, key, value);

            ctx.popBlock();
        });
    }
}
