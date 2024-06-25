/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionStepPointClassification;
import com.optio3.cloud.hub.model.PointClassAssignment;
import com.optio3.cloud.hub.model.normalization.ClassificationReason;

@JsonTypeName("NormalizationEngineStatementSetPointClassTable")
public class NormalizationEngineStatementSetPointClassTable extends EngineStatementFromNormalization
{
    public EngineExpression<EngineValuePrimitiveString> value;

    public List<PointClassAssignment> assignments = Lists.newArrayList();

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, value, EngineValuePrimitiveString.class, (valueRaw) ->
        {
            String value = EngineValuePrimitiveString.extract(valueRaw);

            NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;

            if (assignments != null)
            {
                for (PointClassAssignment assignment : assignments)
                {
                    String regex = assignment.regex != null ? assignment.regex : "";
                    boolean matches = ctx.compileRegex(regex, assignment.caseSensitive)
                                         .matcher(value)
                                         .find();

                    if (matches)
                    {
                        ctx2.state.setPointClassId(assignment.pointClassId, 0, 0, ClassificationReason.RegEx);

                        NormalizationEngineExecutionStepPointClassification step = new NormalizationEngineExecutionStepPointClassification();
                        step.pointClassId             = ctx2.state.pointClassId;
                        step.classificationAssignment = assignment;
                        ctx2.pushStep(step);

                        break;
                    }
                }
            }

            ctx.popBlock();
        });
    }
}
