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

@JsonTypeName("NormalizationEngineExpressionGetDashboardEquipmentName")
public class NormalizationEngineExpressionGetDashboardEquipmentName extends EngineExpressionFromNormalization<EngineValuePrimitiveString>
{
    public NormalizationEngineExpressionGetDashboardEquipmentName()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;
        ctx.popBlock(EngineValuePrimitiveString.create(ctx2.state.equipmentNameFromLegacyImport));
    }
}
