/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValueListConcrete;
import com.optio3.cloud.hub.engine.core.value.EngineValueListIterator;
import com.optio3.cloud.hub.engine.core.value.EngineValueLookupTable;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;

@JsonTypeName("EngineExpressionBinaryLookupTableFilter")
public class EngineExpressionBinaryLookupTableFilter extends EngineOperatorBinaryFromCore<EngineValueList<EngineValuePrimitiveString>, EngineValueLookupTable, EngineValueList<EngineValuePrimitiveString>>
{
    public EngineExpressionBinaryLookupTableFilter()
    {
        super(EngineValueList.typeRef_ListOfStrings);
    }

    @Override
    protected EngineValueList<EngineValuePrimitiveString> computeResult(EngineExecutionContext<?, ?> ctx,
                                                                        EngineExecutionStack stack,
                                                                        EngineValueLookupTable table,
                                                                        EngineValueList<EngineValuePrimitiveString> inputs)
    {
        stack.checkNonNullValue(table, "No Lookup Table");

        EngineValueListConcrete<EngineValuePrimitiveString> res = new EngineValueListConcrete<>();

        if (inputs != null)
        {
            for (EngineValueListIterator<EngineValuePrimitiveString> it = inputs.createIterator(); it.hasNext(); )
            {
                EngineValuePrimitiveString val = it.next();

                if (table.find(EngineValuePrimitiveString.extract(val)) != null)
                {
                    res.elements.add(val);
                }
            }
        }

        return res;
    }
}
