/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine;

import java.util.LinkedList;
import java.util.Map;

import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;

public class EngineExecutionStack
{
    public EngineExecutionStack    parent;
    public LinkedList<EngineValue> childResults;

    public EngineBlock                  block;
    public String                       functionId;
    public Class<? extends EngineValue> expectedResult;

    public EngineBlock.ScratchPad scratchPad;

    public Map<String, EngineValue> localVariablesSetter;
    public Map<String, EngineValue> localVariablesGetter;

    //--//

    public <T extends EngineValue> T popChildResult(Class<T> clz)
    {
        if (childResults == null)
        {
            return null;
        }

        if (childResults.size() == 0)
        {
            childResults = null;
            return null;
        }

        EngineValue val = childResults.remove(0);
        if (val == null)
        {
            return null;
        }

        if (clz.isInstance(val))
        {
            return clz.cast(val);
        }

        throw unexpected("Expecting type %s, got %s",
                         clz.getSimpleName(),
                         val.getClass()
                            .getSimpleName());
    }

    //--//

    public <T extends EngineBlock.ScratchPad> T getScratchPad(Class<T> clz)
    {
        if (scratchPad == null)
        {
            if (clz == EngineBlock.ScratchPad.class) // Peephole optimization for the common case.
            {
                scratchPad = new EngineBlock.ScratchPad();
            }
            else
            {
                scratchPad = Reflection.newInstance(clz);
            }
        }

        return clz.cast(scratchPad);
    }

    //--//

    public <R extends EngineValue> R getNonNullValue(EngineValue val,
                                                     Class<R> clz,
                                                     String failureMessage)
    {
        if (!clz.isInstance(val))
        {
            throw unexpected(failureMessage);
        }

        return clz.cast(val);
    }

    public <R extends EngineValue> void checkNonNullValue(EngineValue val,
                                                          String failureMessage)
    {
        if (val == null)
        {
            throw unexpected(failureMessage);
        }
    }

    //--//

    public RuntimeException unexpected(String fmt,
                                       Object... args)
    {
        return unexpected(String.format(fmt, args));
    }

    public RuntimeException unexpected()
    {
        return unexpected("Invalid stack state");
    }

    public RuntimeException unexpected(String text)
    {
        if (scratchPad != null)
        {
            return Exceptions.newRuntimeException("%s ## context: %s", text, ObjectMappers.toJsonNoThrow(null, scratchPad));
        }
        else
        {
            return Exceptions.newRuntimeException("%s", text);
        }
    }
}
