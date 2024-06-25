/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueDeliveryOptions;
import com.optio3.cloud.hub.model.DeliveryOptions;

@JsonTypeName("AlertEngineInputParameterDeliveryOptions")
public class AlertEngineInputParameterDeliveryOptions extends EngineInputParameterFromAlerts<AlertEngineValueDeliveryOptions>
{
    public DeliveryOptions value;

    //--//

    public AlertEngineInputParameterDeliveryOptions()
    {
        super(AlertEngineValueDeliveryOptions.class);
    }

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(AlertEngineValueDeliveryOptions.create(ctx, value));
    }
}
