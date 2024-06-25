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
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.engine.normalizations.value.NormalizationEngineValueDocument;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;

@JsonTypeName("NormalizationEngineOperatorBinaryScoreTopDocument")
public class NormalizationEngineOperatorBinaryScoreTopDocument extends EngineOperatorBinaryFromNormalization<NormalizationEngineValueDocument, EngineValuePrimitiveString, EngineValueList<NormalizationEngineValueDocument>>
{
    public int minNgram;
    public int maxNgram;
    public int minDocFrequency;

    public NormalizationEngineOperatorBinaryScoreTopDocument()
    {
        super(NormalizationEngineValueDocument.class);
    }

    @Override
    protected NormalizationEngineValueDocument computeResult(EngineExecutionContext<?, ?> ctx,
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
        double       maxScore   = 0;
        int          maxIdx     = 0;

        var scoreInfo = vectorizer.score(docStrings, EngineValuePrimitiveString.extract(text, ""), minDocFrequency);

        for (int i = 0; i < scoreInfo.scores.length; i++)
        {
            double score = scoreInfo.scores[i];
            if (score > maxScore)
            {
                maxScore = score;
                maxIdx   = i;
            }
        }

        var doc = documents.get(maxIdx)
                           .copy();
        doc.score = maxScore;

        if (ctx.isLogEnabled())
        {
            ctx.recordLogEntry(stack, "Score Top Document");
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
            ctx.recordLogEntry(stack, String.format("%s - Score: %f", doc.format(ctx, stack, null), doc.score));
            ctx.recordLogEntry(stack, "");
            ctx.recordLogEntry(stack, "");
        }

        return doc;
    }
}
