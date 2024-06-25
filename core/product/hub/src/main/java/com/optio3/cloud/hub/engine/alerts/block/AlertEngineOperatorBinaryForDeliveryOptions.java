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
import com.optio3.cloud.hub.model.shared.program.CommonEngineSetOperation;

@JsonTypeName("AlertEngineOperatorBinaryForDeliveryOptions")
public class AlertEngineOperatorBinaryForDeliveryOptions extends EngineOperatorBinaryFromAlerts<AlertEngineValueDeliveryOptions, AlertEngineValueDeliveryOptions, AlertEngineValueDeliveryOptions>
{
    public CommonEngineSetOperation operation;

    public AlertEngineOperatorBinaryForDeliveryOptions()
    {
        super(AlertEngineValueDeliveryOptions.class);
    }

    @Override
    protected AlertEngineValueDeliveryOptions computeResult(EngineExecutionContext<?, ?> ctx,
                                                            EngineExecutionStack stack,
                                                            AlertEngineValueDeliveryOptions optionsA,
                                                            AlertEngineValueDeliveryOptions optionsB)
    {
        switch (operation)
        {
            case Add:
                return AlertEngineValueDeliveryOptions.add(optionsA, optionsB);

            case Subtract:
                return AlertEngineValueDeliveryOptions.subtract(optionsA, optionsB);

            case Intersect:
                return AlertEngineValueDeliveryOptions.intersect(optionsA, optionsB);

            default:
                throw stack.unexpected();
        }
    }
}
