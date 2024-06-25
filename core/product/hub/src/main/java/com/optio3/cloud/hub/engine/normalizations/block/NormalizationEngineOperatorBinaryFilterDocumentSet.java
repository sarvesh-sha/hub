/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValueListConcrete;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueDocument;
import com.optio3.cloud.hub.logic.normalizations.TermFrequencyInverseDocumentFrequencyVectorizer;
import com.optio3.util.CollectionUtils;

@JsonTypeName("NormalizationEngineOperatorBinaryFilterDocumentSet")
public class NormalizationEngineOperatorBinaryFilterDocumentSet extends EngineOperatorBinaryFromNormalization<EngineValueList<NormalizationEngineValueDocument>, EngineValueList<NormalizationEngineValueDocument>, EngineValuePrimitiveString>
{
    public List<String> negativeTerms = Lists.newArrayList();

    public NormalizationEngineOperatorBinaryFilterDocumentSet()
    {
        super(NormalizationEngineLiteralDocumentSet.typeRef_ListOfDocument);
    }

    @Override
    protected EngineValueList<NormalizationEngineValueDocument> computeResult(EngineExecutionContext<?, ?> ctx,
                                                                              EngineExecutionStack stack,
                                                                              EngineValueList<NormalizationEngineValueDocument> documents,
                                                                              EngineValuePrimitiveString text)
    {
        if (CollectionUtils.isEmpty(negativeTerms))
        {
            return documents;
        }

        NormalizationEngineExecutionContext             ctx2       = (NormalizationEngineExecutionContext) ctx;
        TermFrequencyInverseDocumentFrequencyVectorizer vectorizer = ctx2.getVectorizer(1, 3);

        EngineValueListConcrete<NormalizationEngineValueDocument> result       = new EngineValueListConcrete<>();
        TermFrequencyInverseDocumentFrequencyVectorizer.Document  textDocument = vectorizer.getDocument(EngineValuePrimitiveString.extract(text, ""));
        Set<String>                                               textTerms    = textDocument.getNgrams();

        for (int i = 0; i < documents.getLength(); i++)
        {
            NormalizationEngineValueDocument                         rawDocument = documents.getNthElement(i);
            TermFrequencyInverseDocumentFrequencyVectorizer.Document document    = vectorizer.getDocument(rawDocument.text);
            Set<String>                                              docTerms    = document.getNgrams();

            boolean include = true;
            for (String term : negativeTerms)
            {
                if (docTerms.contains(term) != textTerms.contains(term))
                {
                    include = false;
                    break;
                }
            }

            if (include)
            {
                result.elements.add(rawDocument);
            }
        }
        return result;
    }
}
