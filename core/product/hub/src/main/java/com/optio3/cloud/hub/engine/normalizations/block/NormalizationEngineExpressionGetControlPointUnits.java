/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueEngineeringUnits;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@JsonTypeName("NormalizationEngineExpressionGetControlPointUnits")
public class NormalizationEngineExpressionGetControlPointUnits extends EngineExpressionFromNormalization<EngineValueEngineeringUnits>
{
    public NormalizationEngineExpressionGetControlPointUnits()
    {
        super(EngineValueEngineeringUnits.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;
        ctx.popBlock(EngineValueEngineeringUnits.create(EngineeringUnitsFactors.get(ctx2.state.controlPointUnits)));
    }
}
