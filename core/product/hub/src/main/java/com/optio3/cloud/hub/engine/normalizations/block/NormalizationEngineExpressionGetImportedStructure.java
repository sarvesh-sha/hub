/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValueListConcrete;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;

@JsonTypeName("NormalizationEngineExpressionGetImportedStructure")
public class NormalizationEngineExpressionGetImportedStructure extends EngineExpressionFromNormalization<EngineValueList<EngineValuePrimitiveString>>
{
    public NormalizationEngineExpressionGetImportedStructure()
    {
        super(EngineValueList.typeRef_ListOfStrings);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        NormalizationEngineExecutionContext ctx2 = (NormalizationEngineExecutionContext) ctx;

        EngineValueListConcrete<EngineValuePrimitiveString> res = new EngineValueListConcrete<>();
        for (String val : ctx2.state.structureFromLegacyImport)
        {
            res.elements.add(EngineValuePrimitiveString.create(val));
        }

        ctx.popBlock(res);
    }
}
