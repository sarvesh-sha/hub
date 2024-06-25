/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPoint;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("AlertEngineInputParameterControlPoint")
public class AlertEngineInputParameterControlPoint extends EngineInputParameterFromAlerts<AlertEngineValueControlPoint>
{
    public TypedRecordIdentity<DeviceElementRecord> value;

    //--//

    public AlertEngineInputParameterControlPoint()
    {
        super(AlertEngineValueControlPoint.class);
    }

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(AlertEngineValueControlPoint.createTyped(value));
    }
}
