/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionStepEquipmentClassification;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueEquipment;
import com.optio3.cloud.hub.model.EquipmentClassAssignment;

@JsonTypeName("NormalizationEngineStatementSetEquipmentClassTable")
public class NormalizationEngineStatementSetEquipmentClassTable extends EngineStatementFromNormalization
{
    public EngineExpression<NormalizationEngineValueEquipment> value;

    public List<EquipmentClassAssignment> assignments = Lists.newArrayList();

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, value, NormalizationEngineValueEquipment.class, (equipment) ->
        {
            NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;

            EquipmentClassAssignment match = EquipmentClassAssignment.matchAssignments(ctx, assignments, equipment);
            if (match != null)
            {
                NormalizationEngineExecutionStepEquipmentClassification step = new NormalizationEngineExecutionStepEquipmentClassification();
                step.equipment                = equipment.copy();
                step.classificationAssignment = match;
                ctx2.pushStep(step);
            }

            ctx.popBlock();
        });
    }
}
