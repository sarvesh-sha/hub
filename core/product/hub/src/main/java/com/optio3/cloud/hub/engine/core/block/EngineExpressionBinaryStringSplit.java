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
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("EngineExpressionBinaryStringSplit")
public class EngineExpressionBinaryStringSplit extends EngineOperatorBinaryFromCore<EngineValueList<EngineValuePrimitiveString>, EngineValuePrimitiveString, EngineValuePrimitiveString>
{
    public EngineExpressionBinaryStringSplit()
    {
        super(EngineValueList.typeRef_ListOfStrings);
    }

    @Override
    protected EngineValueList<EngineValuePrimitiveString> computeResult(EngineExecutionContext<?, ?> ctx,
                                                                        EngineExecutionStack stack,
                                                                        EngineValuePrimitiveString v1,
                                                                        EngineValuePrimitiveString v2)
    {
        String t1 = EngineValuePrimitiveString.extract(v1);
        String t2 = EngineValuePrimitiveString.extract(v2);

        EngineValueListConcrete<EngineValuePrimitiveString> res = new EngineValueListConcrete<>();

        for (String s : StringUtils.split(t1, t2))
        {
            res.elements.add(EngineValuePrimitiveString.create(s));
        }

        return res;
    }
}
