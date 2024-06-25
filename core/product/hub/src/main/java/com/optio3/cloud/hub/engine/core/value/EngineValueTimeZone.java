/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.value;

import java.time.ZoneId;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("EngineValueTimeZone")
public class EngineValueTimeZone extends EngineValue
{
    public String value;

    public static EngineValueTimeZone create(String val)
    {
        if (val == null)
        {
            return null;
        }

        EngineValueTimeZone res = new EngineValueTimeZone();
        res.value = val;
        return res;
    }

    //--//

    @Override
    public int compareTo(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         EngineValue o)
    {
        EngineValueTimeZone other = Reflection.as(o, EngineValueTimeZone.class);
        if (other != null)
        {
            return StringUtils.compare(value, other.value);
        }

        throw stack.unexpected();
    }

    @Override
    public String format(EngineExecutionContext<?, ?> ctx,
                         EngineExecutionStack stack,
                         Map<String, String> modifiers)
    {
        return value;
    }

    public ZoneId resolve(EngineExecutionStack stack)
    {
        return resolve(stack, value);
    }

    public static ZoneId resolve(EngineExecutionStack stack,
                                 String value)
    {
        try
        {
            return value != null ? ZoneId.of(value) : null;
        }
        catch (Throwable t)
        {
            throw stack.unexpected(t.getMessage());
        }
    }
}
