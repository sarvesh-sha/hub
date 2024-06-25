/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueEquipment;
import com.optio3.util.CollectionUtils;

@JsonTypeName("NormalizationEngineExpressionGetEquipment")
public class NormalizationEngineExpressionGetEquipment extends EngineExpressionFromNormalization<EngineValuePrimitiveString>
{
    public NormalizationEngineExpressionGetEquipment()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        NormalizationEngineExecutionContext ctx2       = (NormalizationEngineExecutionContext) ctx;
        NormalizationEngineValueEquipment   firstEquip = CollectionUtils.firstElement(ctx2.state.equipments);
        ctx.popBlock(EngineValuePrimitiveString.create(firstEquip != null ? firstEquip.name : null));
    }
}
