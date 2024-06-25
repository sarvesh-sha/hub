/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import static com.optio3.cloud.hub.engine.core.value.EngineValueList.typeRef_ListOfStrings;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValueListConcrete;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.logic.normalizations.TermFrequencyInverseDocumentFrequencyVectorizer;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("NormalizationEngineOperatorUnaryTokenizeString")
public class NormalizationEngineOperatorUnaryTokenizeString extends EngineOperatorUnaryFromNormalization<EngineValueList<EngineValuePrimitiveString>, EngineValuePrimitiveString>
{
    public NormalizationEngineOperatorUnaryTokenizeString()
    {
        super(typeRef_ListOfStrings);
    }

    @Override
    protected EngineValueList<EngineValuePrimitiveString> computeResult(EngineExecutionContext<?, ?> ctx,
                                                                        EngineExecutionStack stack,
                                                                        EngineValuePrimitiveString value)
    {
        NormalizationEngineExecutionContext                 ctx2   = (NormalizationEngineExecutionContext) ctx;
        EngineValueListConcrete<EngineValuePrimitiveString> result = new EngineValueListConcrete<>();
        String                                              text   = EngineValuePrimitiveString.extract(value);
        if (StringUtils.isNotEmpty(text))
        {
            TermFrequencyInverseDocumentFrequencyVectorizer.Document document = ctx2.getVectorizer(1, 1)
                                                                                    .getDocument(text);

            Set<String> textTags = document.getNgrams();
            result.elements.addAll(CollectionUtils.transformToList(textTags, EngineValuePrimitiveString::create));
        }
        return result;
    }
}
