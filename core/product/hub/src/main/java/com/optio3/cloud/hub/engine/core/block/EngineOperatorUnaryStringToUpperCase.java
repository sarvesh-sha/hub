/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;

@JsonTypeName("EngineOperatorUnaryStringToUpperCase")
public class EngineOperatorUnaryStringToUpperCase extends EngineOperatorUnaryFromCore<EngineValuePrimitiveString, EngineValuePrimitiveString>
{
    public EngineOperatorUnaryStringToUpperCase()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    protected EngineValuePrimitiveString computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       EngineValuePrimitiveString val)
    {
        String str = EngineValuePrimitiveString.extract(val);

        if (str != null)
        {
            str = str.toUpperCase();
        }

        return EngineValuePrimitiveString.create(str);
    }
}
