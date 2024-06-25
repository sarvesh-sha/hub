/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.value;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.serialization.Reflection;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("EngineValueDateTime")
public class EngineValueDateTime extends EngineValue
{
    public ZonedDateTime value;

    public static EngineValueDateTime create(ZonedDateTime val)
    {
        if (val == null)
        {
            return null;
        }

        EngineValueDateTime res = new EngineValueDateTime();
        res.value = val;
        return res;
    }

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        EngineValueDateTime other = Reflection.as(o, EngineValueDateTime.class);
        if (other != null)
        {
            return TimeUtils.compare(value, other.value);
        }

        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return format(stack, modifiers, value);
    }

    public static String format(EngineExecutionStack stack,
                                Map<String, String> modifiers,
                                ZonedDateTime value)
    {
        if (value != null)
        {
            DateTimeFormatter formatter;

            String pattern = modifiers.get("pattern");
            if (StringUtils.isNotBlank(pattern))
            {
                try
                {
                    formatter = DateTimeFormatter.ofPattern(pattern);
                }
                catch (Exception e)
                {
                    throw stack.unexpected("Invalid pattern: %s", pattern);
                }
            }
            else
            {
                formatter = TimeUtils.DEFAULT_FORMATTER_NO_MILLI;
            }

            return formatter.format(value);
        }

        return null;
    }
}
