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
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveBoolean;
import com.optio3.cloud.hub.model.shared.program.CommonEngineCompareOperation;

@JsonTypeName("EngineExpressionBinaryLogicCompare")
public class EngineExpressionBinaryLogicCompare extends EngineOperatorBinaryFromCore<EngineValuePrimitiveBoolean, EngineValue, EngineValue>
{
    public CommonEngineCompareOperation operation;

    public EngineExpressionBinaryLogicCompare()
    {
        super(EngineValuePrimitiveBoolean.class);
    }

    @Override
    protected EngineValuePrimitiveBoolean computeResult(EngineExecutionContext<?, ?> ctx,
                                                        EngineExecutionStack stack,
                                                        EngineValue a,
                                                        EngineValue b)
    {
        int comparison;

        if (a == null)
        {
            comparison = b == null ? 0 : -1;
        }
        else if (b == null)
        {
            comparison = 1;
        }
        else
        {
            comparison = a.compareTo(ctx, stack, b);
        }

        switch (operation)
        {
            case Equal:
                return EngineValuePrimitiveBoolean.create(comparison == 0);

            case NotEqual:
                return EngineValuePrimitiveBoolean.create(comparison != 0);

            case LessThan:
                return EngineValuePrimitiveBoolean.create(comparison < 0);

            case LessThanOrEqual:
                return EngineValuePrimitiveBoolean.create(comparison <= 0);

            case GreaterThan:
                return EngineValuePrimitiveBoolean.create(comparison > 0);

            case GreaterThanOrEqual:
                return EngineValuePrimitiveBoolean.create(comparison >= 0);

            default:
                throw stack.unexpected();
        }
    }
}
