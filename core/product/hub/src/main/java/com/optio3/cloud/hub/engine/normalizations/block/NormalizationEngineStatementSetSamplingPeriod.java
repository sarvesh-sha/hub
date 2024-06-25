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

@JsonTypeName("NormalizationEngineStatementSetSamplingPeriod")
public class NormalizationEngineStatementSetSamplingPeriod extends EngineStatementFromNormalization
{
    public int samplingPeriod;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;
        if (samplingPeriod > 0)
        {
            ctx2.state.samplingPeriod = samplingPeriod;
        }
        else
        {
            ctx2.state.samplingPeriod = 0;
            ctx2.state.noSampling     = true;
        }

        ctx.popBlock();
    }
}
