/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.core.value.EngineValueLookupTable;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;

@JsonTypeName("EngineStatementLookupTablePut")
public class EngineStatementLookupTablePut extends EngineStatementFromCore
{
    public EngineExpression<EngineValueLookupTable>     table;
    public EngineExpression<EngineValuePrimitiveString> key;
    public EngineExpression<EngineValuePrimitiveString> value;
    public boolean                                      caseInsensitive;

    //--//

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack) throws
                                                             Exception
    {
        extractParams(ctx, stack, table, EngineValueLookupTable.class, key, EngineValuePrimitiveString.class, value, EngineValuePrimitiveString.class, (tableVal, keyVal, valueVal) ->
        {
            String key   = EngineValuePrimitiveString.extract(keyVal);
            String value = EngineValuePrimitiveString.extract(valueVal);
            if (tableVal != null && key != null)
            {
                tableVal.put(key, value, caseInsensitive);
            }

            ctx.popBlock();
        });
    }
}
