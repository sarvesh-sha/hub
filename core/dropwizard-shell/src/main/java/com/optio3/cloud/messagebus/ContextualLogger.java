/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.optio3.cloud.JsonWebSocket;
import com.optio3.logging.Logger;
import com.optio3.logging.RedirectingLogger;

abstract class ContextualLogger extends RedirectingLogger
{
    private static final Function<Object, Object> s_processor = (arg) ->
    {
        if (arg == null)
        {
            return null;
        }

        if (arg instanceof String)
        {
            return arg;
        }

        if (arg instanceof Number)
        {
            return arg;
        }

        if (arg instanceof Enum<?>)
        {
            return arg;
        }

        if (arg instanceof Throwable)
        {
            return arg;
        }

        try
        {
            return JsonWebSocket.serializeValue(arg);
        }
        catch (JsonProcessingException e)
        {
            return arg;
        }
    };

    //--//

    ContextualLogger(Logger instance)
    {
        super(instance);
    }

    //--//

    @Override
    protected Function<Object, Object> getArgumentProcessor()
    {
        return s_processor;
    }
}
