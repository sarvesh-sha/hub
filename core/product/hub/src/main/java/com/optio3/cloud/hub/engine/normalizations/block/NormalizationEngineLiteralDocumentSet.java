/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValueListConcrete;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueDocument;

@JsonTypeName("NormalizationEngineLiteralDocumentSet")
public class NormalizationEngineLiteralDocumentSet extends EngineLiteralFromNormalization<EngineValueList<NormalizationEngineValueDocument>>
{
    public static final TypeReference<EngineValueList<NormalizationEngineValueDocument>> typeRef_ListOfDocument = new TypeReference<EngineValueList<NormalizationEngineValueDocument>>()
    {
    };

    public List<NormalizationEngineValueDocument> value;

    //--//

    public NormalizationEngineLiteralDocumentSet()
    {
        super(typeRef_ListOfDocument);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        EngineValueListConcrete<NormalizationEngineValueDocument> list = new EngineValueListConcrete<>();
        if (value != null)
        {
            list.elements.addAll(value);
        }

        ctx.popBlock(list);
    }
}
