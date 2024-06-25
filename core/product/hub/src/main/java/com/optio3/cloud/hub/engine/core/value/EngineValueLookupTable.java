/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.value;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;

@JsonTypeName("EngineValueLookupTable")
public class EngineValueLookupTable extends EngineValue
{
    private Map<String, String> m_mapCaseSensitive;
    private Map<String, String> m_mapCaseInsensitive;

    public static EngineValueLookupTable create(Map<String, String> mapCaseSensitive,
                                                Map<String, String> mapCaseInsensitive)
    {
        EngineValueLookupTable value = new EngineValueLookupTable();
        value.m_mapCaseSensitive   = mapCaseSensitive;
        value.m_mapCaseInsensitive = mapCaseInsensitive;
        return value;
    }

    public String find(String input)
    {
        if (input == null)
        {
            return null;
        }

        String value = m_mapCaseSensitive.get(input);
        if (value != null)
        {
            return value;
        }

        return m_mapCaseInsensitive.get(input.toLowerCase());
    }

    public void put(String key,
                    String value,
                    boolean caseInsensitive)
    {
        if (caseInsensitive)
        {
            m_mapCaseInsensitive.put(key.toLowerCase(), value);
        }
        else
        {
            m_mapCaseSensitive.put(key, value);
        }
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
