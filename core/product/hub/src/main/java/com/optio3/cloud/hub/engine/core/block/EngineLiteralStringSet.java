/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueList;
import com.optio3.cloud.hub.engine.core.value.EngineValueListConcrete;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.util.CollectionUtils;

@JsonTypeName("EngineLiteralStringSet")
public class EngineLiteralStringSet extends EngineLiteralFromCore<EngineValueList<EngineValuePrimitiveString>>
{
    public List<String> value;

    //--//

    public EngineLiteralStringSet()
    {
        super(EngineValueList.typeRef_ListOfStrings);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        EngineValueListConcrete<EngineValuePrimitiveString> list = new EngineValueListConcrete<>();
        list.elements.addAll(CollectionUtils.transformToList(value, EngineValuePrimitiveString::create));

        ctx.popBlock(list);
    }
}
