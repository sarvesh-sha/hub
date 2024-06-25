/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValueListConcrete;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueController;

@JsonTypeName("NormalizationEngineExpressionGetControllers")
public class NormalizationEngineExpressionGetControllers extends EngineExpressionFromNormalization<EngineValueList<NormalizationEngineValueController>>
{
    public static final TypeReference<EngineValueList<NormalizationEngineValueController>> typeRef_ListOfControllers = new TypeReference<EngineValueList<NormalizationEngineValueController>>()
    {
    };

    public NormalizationEngineExpressionGetControllers()
    {
        super(typeRef_ListOfControllers);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;

        EngineValueListConcrete<NormalizationEngineValueController> res = new EngineValueListConcrete<>();

        res.elements.addAll(ctx2.getControllers());

        ctx.popBlock(res);
    }
}
