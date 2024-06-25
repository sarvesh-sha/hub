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
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionStepPointClassification;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueDocument;
import com.optio3.cloud.hub.model.normalization.ClassificationReason;

@JsonTypeName("NormalizationEngineStatementSetPointClassFromDocument")
public class NormalizationEngineStatementSetPointClassFromDocument extends EngineStatementFromNormalization
{
    public EngineExpression<NormalizationEngineValueDocument> value;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, value, NormalizationEngineValueDocument.class, (document) ->
        {

            NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;

            ctx2.state.setPointClassId(document.id, document.score, 0, ClassificationReason.TFIDFVectorModel);
            NormalizationEngineExecutionStepPointClassification step = new NormalizationEngineExecutionStepPointClassification();
            step.pointClassId           = ctx2.state.pointClassId;
            step.classificationDocument = document;
            ctx2.pushStep(step);

            ctx.popBlock();
        });
    }
}
