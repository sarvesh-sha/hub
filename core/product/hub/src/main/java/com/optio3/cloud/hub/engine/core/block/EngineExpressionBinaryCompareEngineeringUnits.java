/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueEngineeringUnits;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveBoolean;
import com.optio3.cloud.hub.model.shared.program.CommonEngineCompareOperation;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@JsonTypeName("EngineExpressionBinaryCompareEngineeringUnits")
public class EngineExpressionBinaryCompareEngineeringUnits extends EngineOperatorBinaryFromCore<EngineValuePrimitiveBoolean, EngineValueEngineeringUnits, EngineValueEngineeringUnits>
{
    public CommonEngineCompareOperation operation;

    public EngineExpressionBinaryCompareEngineeringUnits()
    {
        super(EngineValuePrimitiveBoolean.class);
    }

    @Override
    protected EngineValuePrimitiveBoolean computeResult(EngineExecutionContext<?, ?> ctx,
                                                        EngineExecutionStack stack,
                                                        EngineValueEngineeringUnits a,
                                                        EngineValueEngineeringUnits b)
    {
        boolean equal = false;

        if (a == null && b == null)
        {
            equal = true;
        }
        else if (a != null && b != null)
        {
            equal = EngineeringUnitsFactors.areIdentical(a.value, b.value);
        }

        switch (operation)
        {
            case Equal:
                return EngineValuePrimitiveBoolean.create(equal);

            case NotEqual:
                return EngineValuePrimitiveBoolean.create(!equal);

            default:
                throw stack.unexpected();
        }
    }
}
