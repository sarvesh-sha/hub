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
import com.optio3.cloud.hub.engine.core.value.EngineValueListIterator;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;

@JsonTypeName("EngineExpressionBinaryListJoin")
public class EngineExpressionBinaryListJoin extends EngineOperatorBinaryFromCore<EngineValuePrimitiveString, EngineValueList<EngineValuePrimitiveString>, EngineValuePrimitiveString>
{
    public EngineExpressionBinaryListJoin()
    {
        super(EngineValuePrimitiveString.class);
    }

    @Override
    protected EngineValuePrimitiveString computeResult(EngineExecutionContext<?, ?> ctx,
                                                       EngineExecutionStack stack,
                                                       EngineValueList<EngineValuePrimitiveString> lst,
                                                       EngineValuePrimitiveString separator)
    {
        String res;

        if (lst == null)
        {
            res = "";
        }
        else
        {
            StringBuilder sb    = new StringBuilder();
            String        sep   = EngineValuePrimitiveString.extract(separator);
            boolean       first = true;

            for (EngineValueListIterator<EngineValuePrimitiveString> it = lst.createIterator(); it.hasNext(); )
            {
                EngineValuePrimitiveString val = it.next();

                if (first)
                {
                    first = false;
                }
                else
                {
                    sb.append(sep);
                }

                sb.append(EngineValuePrimitiveString.extract(val, ""));
            }

            res = sb.toString();
        }

        return EngineValuePrimitiveString.create(res);
    }
}
