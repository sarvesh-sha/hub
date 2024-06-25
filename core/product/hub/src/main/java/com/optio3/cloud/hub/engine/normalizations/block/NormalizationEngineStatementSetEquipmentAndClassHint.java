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

@JsonTypeName("NormalizationEngineStatementSetEquipmentAndClassHint")
public class NormalizationEngineStatementSetEquipmentAndClassHint extends EngineStatementFromNormalization
{
    public EngineExpression<EngineValuePrimitiveString> value;
    public EngineExpression<EngineValuePrimitiveString> hint;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, value, EngineValuePrimitiveString.class, hint, EngineValuePrimitiveString.class, (valueRaw, hintRaw) ->
        {
            String value = EngineValuePrimitiveString.extract(valueRaw);
            String hint  = EngineValuePrimitiveString.extract(hintRaw);

            NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;
            ctx2.state.equipments.clear();
            ctx2.state.equipments.add(NormalizationEngineValueEquipment.create(value, null, hint));

            ctx.popBlock();
        });
    }
}
