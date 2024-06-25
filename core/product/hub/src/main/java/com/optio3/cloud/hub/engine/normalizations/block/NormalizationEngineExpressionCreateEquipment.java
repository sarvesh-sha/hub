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
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueEquipment;

@JsonTypeName("NormalizationEngineExpressionCreateEquipment")
public class NormalizationEngineExpressionCreateEquipment extends EngineExpressionFromNormalization<NormalizationEngineValueEquipment>
{
    public EngineExpression<EngineValuePrimitiveString> value;

    public String equipmentClassId;

    //--//

    public NormalizationEngineExpressionCreateEquipment()
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

            NormalizationEngineValueEquipment equipment = ctx2.pushEquipment(valueRaw, equipmentClassId, true);

            ctx.popBlock(equipment);
        });
    }
}