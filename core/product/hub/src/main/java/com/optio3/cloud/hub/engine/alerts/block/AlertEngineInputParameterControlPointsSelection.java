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
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPoints;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.model.ControlPointsSelection;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("AlertEngineInputParameterControlPointsSelection")
public class AlertEngineInputParameterControlPointsSelection extends EngineInputParameterFromAlerts<EngineValueList>
{
    public ControlPointsSelection value;

    //--//

    public AlertEngineInputParameterControlPointsSelection()
    {
        super(EngineValueList.class);
    }

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        AlertEngineValueControlPoints res = new AlertEngineValueControlPoints();

        for (TypedRecordIdentity<DeviceElementRecord> controlPoint : value.identities)
        {
            res.elements.add(AlertEngineValueControlPoint.createTyped(controlPoint));
        }

        ctx.popBlock(res);
    }
}
