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
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionStepPointClassification;
import com.optio3.cloud.hub.logic.normalizations.NormalizationScore;
import com.optio3.cloud.hub.logic.normalizations.PointClass;
import com.optio3.cloud.hub.model.normalization.ClassificationReason;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;

@JsonTypeName("NormalizationEngineStatementSetPointClassFromTermScoring")
public class NormalizationEngineStatementSetPointClassFromTermScoring extends EngineStatementFromNormalization
{
    public EngineExpression<EngineValuePrimitiveString> value;

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, value, EngineValuePrimitiveString.class, (value) ->
        {
            NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;

            if (ctx2.state.pointClassId == null && !ctx2.state.setUnclassified)
            {
                BACnetObjectType type;
                try
                {
                    type = BACnetObjectType.parse(ctx2.state.controlPointType);
                }
                catch (Throwable t)
                {
                    type = null;
                }

                NormalizationScore.Context<PointClass> classificationCtx = ctx2.normalizationEngine.scoreTopPointClass(EngineValuePrimitiveString.extract(value, ""),
                                                                                                                       PointClass.filterBasedOnObjectType(type),
                                                                                                                       PointClass.boostScoreOnUnitsMatch(ctx2.state.controlPointUnits));

                if (classificationCtx != null)
                {
                    ctx2.state.setPointClassId(classificationCtx.context.idAsString(), classificationCtx.score.positiveScore, classificationCtx.score.negativeScore, ClassificationReason.TermScoring);
                    NormalizationEngineExecutionStepPointClassification step = new NormalizationEngineExecutionStepPointClassification();
                    step.pointClassId = ctx2.state.pointClassId;
                    ctx2.pushStep(step);
                }
            }
            ctx.popBlock();
        });
    }
}
