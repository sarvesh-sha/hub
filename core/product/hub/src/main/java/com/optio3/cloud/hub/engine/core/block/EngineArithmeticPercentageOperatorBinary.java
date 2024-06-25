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
import com.optio3.cloud.hub.model.shared.program.CommonEngineArithmeticOperation;

@JsonTypeName("EngineArithmeticPercentageOperatorBinary")
public class EngineArithmeticPercentageOperatorBinary extends EngineOperatorBinaryFromCore<EngineValuePrimitiveNumber, EngineValuePrimitiveNumber, EngineValuePrimitiveNumber>
{
    public CommonEngineArithmeticOperation operation;

    public EngineArithmeticPercentageOperatorBinary()
    {
        super(EngineValuePrimitiveNumber.class);
    }

    @Override
    protected EngineValuePrimitiveNumber computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       EngineValuePrimitiveNumber primitiveA,
                                                       EngineValuePrimitiveNumber primitiveB)
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
                return EngineValuePrimitiveNumber.create(primitiveA.asNumber() * (100 + primitiveB.asNumber()) / 100.0);

            case Minus:
                return EngineValuePrimitiveNumber.create(primitiveA.asNumber() * (100 - primitiveB.asNumber()) / 100.0);

            default:
                throw stack.unexpected();
        }
    }
}
