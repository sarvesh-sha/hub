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
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;

@JsonTypeName("EngineExpressionBinaryListGet")
public class EngineExpressionBinaryListGet extends EngineOperatorBinaryFromCore<EngineValue, EngineValueList<EngineValue>, EngineValuePrimitiveNumber>
{
    public EngineExpressionBinaryListGet()
    {
        super(EngineValue.class);
    }

    @Override
    protected EngineValue computeResult(EngineExecutionContext<?, ?> ctx,
                                        EngineExecutionStack stack,
                                        EngineValueList<EngineValue> lst,
                                        EngineValuePrimitiveNumber pos)
    {
        return lst != null ? lst.getNthElement((int) pos.value) : null;
    }
}
