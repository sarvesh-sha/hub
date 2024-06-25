/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.value;

import java.util.Map;

import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;

public abstract class EngineValueListIterator<T extends EngineValue> extends EngineValue
{
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

    public abstract boolean hasNext();

    public abstract T next();
}
