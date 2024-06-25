/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionStepUnits;
import com.optio3.protocol.model.EngineeringUnits;

@JsonTypeName("NormalizationEngineStatementSetEngineeringUnits")
public class NormalizationEngineStatementSetEngineeringUnits extends EngineStatementFromNormalization
{
    public EngineeringUnits units;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;

        ctx2.state.controlPointUnits = units;

        NormalizationEngineExecutionStepUnits step = new NormalizationEngineExecutionStepUnits();
        step.units = units;
        ctx2.pushStep(step);

        ctx.popBlock();
    }
}
