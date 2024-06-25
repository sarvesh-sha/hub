/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.value;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;

@JsonTypeName("EngineValueRegexMatch")
public class EngineValueRegexMatch extends EngineValue
{
    public String  regEx;
    public String  input;
    public boolean caseSensitive;

    private Pattern m_pattern;
    private Matcher m_matcher;
    private boolean m_isMatch;

    //--//

    public static EngineValueRegexMatch create(String regEx,
                                               String input,
                                               boolean caseSensitive)
    {
        if (regEx == null)
        {
            return null;
        }

        EngineValueRegexMatch res = new EngineValueRegexMatch();
        res.regEx         = regEx;
        res.input         = input;
        res.caseSensitive = caseSensitive;
        return res;
    }

    //--//

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

    //--//

    public boolean isMatch(EngineExecutionContext<?, ?> ctx)
    {
        ensurePattern(ctx);

        return m_isMatch;
    }

    public String getGroup(EngineExecutionContext<?, ?> ctx,
                           int group)
    {
        return isMatch(ctx) ? m_matcher.group(group) : null;
    }

    public String replace(EngineExecutionContext<?, ?> ctx,
                          String replaceText)
    {
        if (!isMatch(ctx))
        {
            return null;
        }

        String res = m_matcher.replaceAll(replaceText);

        // replaceAll resets the groups, flush the cached pattern.
        m_pattern = null;
        return res;
    }

    //--//

    private void ensurePattern(EngineExecutionContext<?, ?> ctx)
    {
        if (m_pattern == null)
        {
            m_pattern = ctx.compileRegex(regEx, caseSensitive);
            m_matcher = m_pattern.matcher(input);

            m_isMatch = m_matcher.find();
        }
    }
}
