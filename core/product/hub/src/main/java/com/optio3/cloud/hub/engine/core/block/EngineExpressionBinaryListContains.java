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
import com.optio3.cloud.hub.engine.core.value.EngineValueListIterator;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveBoolean;

@JsonTypeName("EngineExpressionBinaryListContains")
public class EngineExpressionBinaryListContains extends EngineOperatorBinaryFromCore<EngineValuePrimitiveBoolean, EngineValueList<EngineValue>, EngineValue>
{
    public EngineExpressionBinaryListContains()
    {
        super(EngineValuePrimitiveBoolean.class);
    }

    @Override
    protected EngineValuePrimitiveBoolean computeResult(EngineExecutionContext<?, ?> ctx,
                                                        EngineExecutionStack stack,
                                                        EngineValueList<EngineValue> lst,
                                                        EngineValue targetValue)
    {
        if (lst != null)
        {
            for (EngineValueListIterator<EngineValue> it = lst.createIterator(); it.hasNext(); )
            {
                EngineValue val = it.next();

                if (EngineValue.equals(ctx, stack, val, targetValue))
                {
                    return EngineValuePrimitiveBoolean.create(true);
                }
            }
        }

        return EngineValuePrimitiveBoolean.create(false);
    }
}
