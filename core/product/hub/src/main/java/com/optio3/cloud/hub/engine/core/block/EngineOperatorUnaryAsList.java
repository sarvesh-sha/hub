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
import com.optio3.serialization.Reflection;

@JsonTypeName("EngineOperatorUnaryAsList")
public class EngineOperatorUnaryAsList extends EngineOperatorUnaryFromCore<EngineValueList, EngineValue>
{
    public EngineOperatorUnaryAsList()
    {
        super(EngineValueList.class);
    }

    @Override
    protected EngineValueList computeResult(EngineExecutionContext<?, ?> ctx,
                                            EngineExecutionStack stack,
                                            EngineValue val)
    {
        return Reflection.as(val, EngineValueList.class);
    }
}
