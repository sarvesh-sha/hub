/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionStepPushEquipment;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueEquipment;

@JsonTypeName("NormalizationEngineExpressionCreateChildEquipment")
public class NormalizationEngineExpressionCreateChildEquipment extends EngineExpressionFromNormalization<NormalizationEngineValueEquipment>
{
    public EngineExpression<NormalizationEngineValueEquipment> parent;

    public EngineExpression<EngineValuePrimitiveString> name;

    public String equipmentClassId;

    //--//

    public NormalizationEngineExpressionCreateChildEquipment()
    {
        super(NormalizationEngineValueEquipment.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, name, EngineValuePrimitiveString.class, parent, NormalizationEngineValueEquipment.class, (valueRaw, parentRaw) ->
        {
            NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;

            String value = EngineValuePrimitiveString.extract(valueRaw);

            NormalizationEngineValueEquipment equipment = NormalizationEngineValueEquipment.create(value, equipmentClassId, null);

            parentRaw.addChild(equipment);

            NormalizationEngineExecutionStepPushEquipment step = new NormalizationEngineExecutionStepPushEquipment();
            step.parentEquipment = parentRaw;
            step.equipment       = equipment;
            ctx2.pushStep(step);

            ctx.popBlock(equipment);
        });
    }
}
