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
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionStepEquipmentClassification;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueEquipment;
import com.optio3.cloud.hub.model.EquipmentClassAssignment;

@JsonTypeName("NormalizationEngineExpressionPushEquipmentTable")
public class NormalizationEngineExpressionPushEquipmentTable extends EngineExpressionFromNormalization<NormalizationEngineValueEquipment>
{
    public EngineExpression<EngineValuePrimitiveString> value;

    public List<EquipmentClassAssignment> assignments = Lists.newArrayList();

    //--//

    public NormalizationEngineExpressionPushEquipmentTable()
    {
        super(NormalizationEngineValueEquipment.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, value, EngineValuePrimitiveString.class, (valueRaw) ->
        {
            NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;

            NormalizationEngineValueEquipment newEquip = NormalizationEngineValueEquipment.create(valueRaw.value, null, null);

            EquipmentClassAssignment match = EquipmentClassAssignment.matchAssignments(ctx, assignments, newEquip);

            if (match != null)
            {
                ctx2.pushEquipment(newEquip, false);

                NormalizationEngineExecutionStepEquipmentClassification step = new NormalizationEngineExecutionStepEquipmentClassification();
                step.equipment                = newEquip.copy();
                step.classificationAssignment = match;
                ctx2.pushStep(step);
            }

            ctx.popBlock(newEquip);
        });
    }
}
