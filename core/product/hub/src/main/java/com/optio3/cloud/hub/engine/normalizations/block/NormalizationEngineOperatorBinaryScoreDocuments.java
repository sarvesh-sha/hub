/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.block;

import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValueListConcrete;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueDocument;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;

@JsonTypeName("NormalizationEngineOperatorBinaryScoreDocuments")
public class NormalizationEngineOperatorBinaryScoreDocuments extends EngineOperatorBinaryFromNormalization<EngineValueList<NormalizationEngineValueDocument>, EngineValuePrimitiveString, EngineValueList<NormalizationEngineValueDocument>>
{
    public int minNgram;
    public int maxNgram;
    public int minDocFrequency;

    public double minScore;

    public NormalizationEngineOperatorBinaryScoreDocuments()
    {
        super(NormalizationEngineLiteralDocumentSet.typeRef_ListOfDocument);
    }

    @Override
    protected EngineValueList<NormalizationEngineValueDocument> computeResult(EngineExecutionContext<?, ?> ctx,
                                                                              EngineExecutionStack stack,
                                                                              EngineValuePrimitiveString text,
                                                                              EngineValueList<NormalizationEngineValueDocument> docs)
    {
        NormalizationEngineExecutionContext ctx2       = (NormalizationEngineExecutionContext) ctx;
        var                                 vectorizer = ctx2.getVectorizer(minNgram, maxNgram);

        List<NormalizationEngineValueDocument> documents = Lists.newArrayList();
        for (int i = 0; i < docs.getLength(); i++)
        {
            documents.add(docs.getNthElement(i));
        }

        List<String> docStrings = CollectionUtils.transformToList(documents, (d) -> d.text);

        var result = new EngineValueListConcrete<NormalizationEngineValueDocument>();

        var scoreInfo = vectorizer.score(docStrings, EngineValuePrimitiveString.extract(text, ""), minDocFrequency);

        for (int i = 0; i < scoreInfo.scores.length; i++)
        {
            double score = scoreInfo.scores[i];
            if (score > minScore)
            {
                var doc = documents.get(i)
                                   .copy();
                doc.score = score;
                result.elements.add(doc);
            }
        }

        result.elements.sort(Comparator.comparingDouble(doc -> -1 * doc.score));

        if (ctx.isLogEnabled())
        {
            ctx.recordLogEntry(stack, "Score Documents");
            ctx.recordLogEntry(stack, String.format("Terms: %s", ObjectMappers.prettyPrintAsJson(scoreInfo.terms)));
            ctx.recordLogEntry(stack, String.format("Documents Matrix: %s", ObjectMappers.prettyPrintAsJson(scoreInfo.documentMatrix)));
            for (int i = 0; i < documents.size(); i++)
            {
                String documentText = documents.get(i)
                                               .format(ctx, stack, null);
                ctx.recordLogEntry(stack, String.format("%s: %s", documentText, ObjectMappers.prettyPrintAsJson(scoreInfo.documentMatrix[i])));
            }
            ctx.recordLogEntry(stack, String.format("Text Matrix: %s", ObjectMappers.prettyPrintAsJson(scoreInfo.textResult)));
            ctx.recordLogEntry(stack, "Results:");
            for (NormalizationEngineValueDocument doc : result.elements)
            {
                ctx.recordLogEntry(stack, String.format("%s - Score: %f", doc.format(ctx, stack, null), doc.score));
            }
            ctx.recordLogEntry(stack, "");
            ctx.recordLogEntry(stack, "");
        }

        return result;
    }
}
