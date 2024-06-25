/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineValue;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitive;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;
import com.optio3.serialization.Reflection;

@JsonTypeName("EngineOperatorUnaryListLength")
public class EngineOperatorUnaryListLength extends EngineOperatorUnaryFromCore<EngineValuePrimitiveNumber, EngineValue>
{
    public EngineOperatorUnaryListLength()
    {
        super(EngineValuePrimitiveNumber.class);
    }

    @Override
    protected EngineValuePrimitiveNumber computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       EngineValue val)
    {
        EngineValueList<?> lst = Reflection.as(val, EngineValueList.class);
        if (lst != null)
        {
            return EngineValuePrimitiveNumber.create(lst.getLength());
        }

        EngineValuePrimitive primitive = Reflection.as(val, EngineValuePrimitive.class);
        if (primitive != null)
        {
            return EngineValuePrimitiveNumber.create(primitive.asString()
                                                              .length());
        }

        return EngineValuePrimitiveNumber.create(0);
    }
}
