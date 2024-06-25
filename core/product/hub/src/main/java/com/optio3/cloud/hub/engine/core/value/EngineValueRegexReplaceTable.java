/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.value;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.model.RegexReplacement;

@JsonTypeName("EngineValueRegexReplaceTable")
public class EngineValueRegexReplaceTable extends EngineValue
{
    public List<RegexReplacement> replacements = Lists.newArrayList();

    public static EngineValueRegexReplaceTable create(List<RegexReplacement> replacements)
    {
        EngineValueRegexReplaceTable value = new EngineValueRegexReplaceTable();
        value.replacements = replacements;
        return value;
    }

    public String replace(EngineExecutionContext<?, ?> ctx,
                          String input)
    {
        for (RegexReplacement replacement : replacements)
        {
            if (replacement.regex != null)
            {
                input = ctx.compileRegex(replacement.regex, replacement.caseSensitive)
                           .matcher(input)
                           .replaceAll(replacement.replacement != null ? replacement.replacement : "");
            }
        }

        return input;
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
        return null;
    }
}
