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
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValuePoint;

@JsonTypeName("NormalizationEngineOperatorUnaryGetControllerPoints")
public class NormalizationEngineOperatorUnaryGetControllerPoints extends EngineOperatorUnaryFromNormalization<EngineValueList<NormalizationEngineValuePoint>, NormalizationEngineValueController>
{
    public static final TypeReference<EngineValueList<NormalizationEngineValuePoint>> typeRef_ListOfPoints = new TypeReference<EngineValueList<NormalizationEngineValuePoint>>()
    {
    };

    public NormalizationEngineOperatorUnaryGetControllerPoints()
    {
        super(typeRef_ListOfPoints);
    }

    @Override
    protected EngineValueList<NormalizationEngineValuePoint> computeResult(EngineExecutionContext<?, ?> ctx,
                                                                           EngineExecutionStack stack,
                                                                           NormalizationEngineValueController value) throws
                                                                                                                     Exception
    {
        NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;

        EngineValueListConcrete<NormalizationEngineValuePoint> res = new EngineValueListConcrete<>();

        res.elements.addAll(ctx2.getControllerPoints(value));

        return res;
    }
}
