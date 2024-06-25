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

@JsonTypeName("EngineOperatorUnaryStringToNumber")
public class EngineOperatorUnaryStringToNumber extends EngineOperatorUnaryFromCore<EngineValuePrimitiveNumber, EngineValuePrimitiveString>
{
    public EngineOperatorUnaryStringToNumber()
    {
        super(EngineValuePrimitiveNumber.class);
    }

    @Override
    protected EngineValuePrimitiveNumber computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       EngineValuePrimitiveString val)
    {
        String str = EngineValuePrimitiveString.extract(val);

        return EngineValuePrimitiveNumber.create(str != null ? Double.parseDouble(str) : 0);
    }
}
