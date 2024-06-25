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
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionStepPointClassification;
import com.optio3.cloud.hub.model.normalization.ClassificationReason;

@JsonTypeName("NormalizationEngineStatementSetPointClass")
public class NormalizationEngineStatementSetPointClass extends EngineStatementFromNormalization
{
    public String pointClassId;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;

        ctx2.state.setPointClassId(pointClassId, 0, 0, ClassificationReason.Logic);

        NormalizationEngineExecutionStepPointClassification step = new NormalizationEngineExecutionStepPointClassification();
        step.pointClassId = ctx2.state.pointClassId;
        ctx2.pushStep(step);

        ctx.popBlock();
    }
}
