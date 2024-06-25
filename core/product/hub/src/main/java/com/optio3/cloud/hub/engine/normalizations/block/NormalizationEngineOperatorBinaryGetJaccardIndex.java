/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueDocument;

@JsonTypeName("NormalizationEngineOperatorBinaryGetJaccardIndex")
public class NormalizationEngineOperatorBinaryGetJaccardIndex extends EngineOperatorBinaryFromNormalization<EngineValuePrimitiveNumber, EngineValuePrimitiveString, NormalizationEngineValueDocument>
{
    public NormalizationEngineOperatorBinaryGetJaccardIndex()
    {
        super(EngineValuePrimitiveNumber.class);
    }

    @Override
    protected EngineValuePrimitiveNumber computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       EngineValuePrimitiveString text,
                                                       NormalizationEngineValueDocument document)
    {
        NormalizationEngineExecutionContext ctx2       = (NormalizationEngineExecutionContext) ctx;
        var                                 vectorizer = ctx2.getVectorizer(1, 1);

        var textTerms = vectorizer.getDocument(EngineValuePrimitiveString.extract(text, ""))
                                  .getNgrams();
        var docTerms = vectorizer.getDocument(document.text)
                                 .getNgrams();

        double intersection = Sets.intersection(textTerms, docTerms)
                                  .size();
        double union = Sets.union(textTerms, docTerms)
                           .size();

        return EngineValuePrimitiveNumber.create(intersection / union);
    }
}
