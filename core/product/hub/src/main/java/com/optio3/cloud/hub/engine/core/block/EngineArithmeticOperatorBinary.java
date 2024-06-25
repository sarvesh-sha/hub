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
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;
import com.optio3.cloud.hub.model.shared.program.CommonEngineArithmeticOperation;

@JsonTypeName("EngineArithmeticOperatorBinary")
public class EngineArithmeticOperatorBinary extends EngineOperatorBinaryFromCore<EngineValuePrimitive, EngineValuePrimitive, EngineValuePrimitive>
{
    public CommonEngineArithmeticOperation operation;

    public EngineArithmeticOperatorBinary()
    {
        super(EngineValuePrimitive.class);
    }

    @Override
    protected EngineValuePrimitive computeResult(EngineExecutionContext<?, ?> ctx,
                                                 EngineExecutionStack stack,
                                                 EngineValuePrimitive primitiveA,
                                                 EngineValuePrimitive primitiveB)
    {
        if (primitiveA == null)
        {
            primitiveA = EngineValuePrimitiveNumber.create(0);
        }
        if (primitiveB == null)
        {
            primitiveB = EngineValuePrimitiveNumber.create(0);
        }

        switch (operation)
        {
            case Plus:
                return EngineValuePrimitiveNumber.create(primitiveA.asNumber() + primitiveB.asNumber());

            case Minus:
                return EngineValuePrimitiveNumber.create(primitiveA.asNumber() - primitiveB.asNumber());

            case Multiply:
                return EngineValuePrimitiveNumber.create(primitiveA.asNumber() * primitiveB.asNumber());

            case Divide:
                return EngineValuePrimitiveNumber.create(primitiveA.asNumber() / primitiveB.asNumber());

            case Power:
                return EngineValuePrimitiveNumber.create(Math.pow(primitiveA.asNumber(), primitiveB.asNumber()));

            default:
                throw stack.unexpected();
        }
    }
}
