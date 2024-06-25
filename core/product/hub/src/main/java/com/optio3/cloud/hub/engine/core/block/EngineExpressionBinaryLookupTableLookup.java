/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueLookupTable;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;

@JsonTypeName("EngineExpressionBinaryLookupTableLookup")
public class EngineExpressionBinaryLookupTableLookup extends EngineOperatorBinaryFromCore<EngineValuePrimitiveString, EngineValueLookupTable, EngineValuePrimitiveString>
{
    public EngineExpressionBinaryLookupTableLookup()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    protected EngineValuePrimitiveString computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       EngineValueLookupTable table,
                                                       EngineValuePrimitiveString input)
    {
        stack.checkNonNullValue(table, "No Lookup Table");

        String replacement = table.find(EngineValuePrimitiveString.extract(input));
        return EngineValuePrimitiveString.create(replacement);
    }
}
