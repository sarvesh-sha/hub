/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitive;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveBoolean;

@JsonTypeName("EngineExpressionBinaryApproximateEquality")
public class EngineExpressionBinaryApproximateEquality extends EngineOperatorBinaryFromCore<EngineValuePrimitiveBoolean, EngineValuePrimitive, EngineValuePrimitive>
{
    public double epsilon;

    public EngineExpressionBinaryApproximateEquality()
    {
        super(EngineValuePrimitiveBoolean.class);
    }

    @Override
    protected EngineValuePrimitiveBoolean computeResult(EngineExecutionContext<?, ?> ctx,
                                                        EngineExecutionStack stack,
                                                        EngineValuePrimitive a,
                                                        EngineValuePrimitive b)
    {
        boolean comparison;

        if (a == null)
        {
            comparison = b == null;
        }
        else if (b == null)
        {
            comparison = false;
        }
        else
        {
            double diff    = Math.abs(a.asNumber() - b.asNumber());
            double epsilon = Math.abs(this.epsilon);

            comparison = diff < epsilon;
        }

        return EngineValuePrimitiveBoolean.create(comparison);
    }
}
