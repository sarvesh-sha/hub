/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.value;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueSamples;
import com.optio3.util.BoxingUtils;

@JsonTypeName("EngineValueList")
@JsonSubTypes({ @JsonSubTypes.Type(value = AlertEngineValueSamples.class), @JsonSubTypes.Type(value = EngineValueListConcrete.class) })
public abstract class EngineValueList<T extends EngineValue> extends EngineValue
{
    public static final TypeReference<EngineValueList<EngineValuePrimitiveString>> typeRef_ListOfStrings = new TypeReference<EngineValueList<EngineValuePrimitiveString>>()
    {
    };

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
        StringBuilder sb    = new StringBuilder();
        String        sep   = BoxingUtils.get(modifiers.get("separator"), ", ");
        boolean       first = true;

        for (EngineValueListIterator<T> it = createIterator(); it.hasNext(); )
        {
            T val = it.next();
            if (val != null)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    sb.append(sep);
                }

                sb.append(val.format(ctx, stack, modifiers));
            }
        }

        return sb.toString();
    }

    public abstract int getLength();

    public abstract EngineValueListIterator<T> createIterator();

    public abstract T getNthElement(int pos);
}
