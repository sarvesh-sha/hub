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
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStepSetControlPointValue;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueControlPoint;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitive;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@JsonTypeName("AlertEngineStatementSetProperty")
public class AlertEngineStatementSetProperty extends EngineStatementFromAlerts
{
    public AlertEngineSampleProperty property;
    public EngineeringUnitsFactors   unitsFactors;

    public EngineExpression<AlertEngineValueControlPoint> controlPoint;
    public EngineExpression<EngineValuePrimitive>         value;

    //--//

    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, controlPoint, AlertEngineValueControlPoint.class, value, EngineValuePrimitive.class, (controlPoint, value) ->
        {
            stack.checkNonNullValue(controlPoint, "No Control Point");

            AlertEngineExecutionContext ctx2 = (AlertEngineExecutionContext) ctx;

            var step = ctx2.findStep(AlertEngineExecutionStepSetControlPointValue.class, (oldStep) ->
            {
                if (oldStep.property == property && EngineeringUnitsFactors.areIdentical(oldStep.unitsFactors, unitsFactors))
                {
                    if (RecordIdentity.sameRecord(controlPoint.record, oldStep.controlPoint))
                    {
                        return true;
                    }
                }

                return false;
            });

            if (step == null || value.compareTo(ctx, stack, step.value) != 0)
            {
                step              = new AlertEngineExecutionStepSetControlPointValue();
                step.controlPoint = controlPoint.record;
                step.property     = property;
                step.value        = value;
                step.unitsFactors = unitsFactors;

                ctx2.pushStep(step);
            }

            ctx.popBlock();
        });
    }
}
