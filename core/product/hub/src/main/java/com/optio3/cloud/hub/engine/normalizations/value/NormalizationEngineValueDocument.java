/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.normalizations.value;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;

@JsonTypeName("NormalizationEngineValueDocument")
public class NormalizationEngineValueDocument extends EngineValue
{
    public String id;
    public String text;

    public double score;

    public static NormalizationEngineValueDocument create(String id,
                                                          String text,
                                                          double score)
    {
        NormalizationEngineValueDocument res = new NormalizationEngineValueDocument();
        res.id    = id;
        res.text  = text;
        res.score = score;
        return res;
    }

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return String.format("%s - %s", id, text);
    }

    public NormalizationEngineValueDocument copy()
    {
        return create(id, text, score);
    }
}
