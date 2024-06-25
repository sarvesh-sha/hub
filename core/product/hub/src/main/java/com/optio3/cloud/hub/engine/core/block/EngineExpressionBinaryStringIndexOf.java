/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;

@JsonTypeName("EngineExpressionBinaryStringIndexOf")
public class EngineExpressionBinaryStringIndexOf extends EngineOperatorBinaryFromCore<EngineValuePrimitiveNumber, EngineValuePrimitiveString, EngineValuePrimitiveString>
{
    public EngineExpressionBinaryStringIndexOf()
    {
        super(EngineValuePrimitiveNumber.class);
    }

    @Override
    protected EngineValuePrimitiveNumber computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       EngineValuePrimitiveString v1,
                                                       EngineValuePrimitiveString v2)
    {
        String t1 = EngineValuePrimitiveString.extract(v1);
        String t2 = EngineValuePrimitiveString.extract(v2);

        return EngineValuePrimitiveNumber.create(t1 != null && t2 != null ? t1.indexOf(t2) : -1);
    }
}
