/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueRegexReplaceTable;
import com.optio3.cloud.hub.model.RegexReplacement;

@JsonTypeName("EngineLiteralRegexReplaceTable")
public class EngineLiteralRegexReplaceTable extends EngineLiteralFromCore<EngineValueRegexReplaceTable>
{
    public List<RegexReplacement> replacements = Lists.newArrayList();

    //--//

    public EngineLiteralRegexReplaceTable()
    {
        super(EngineValueRegexReplaceTable.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(EngineValueRegexReplaceTable.create(replacements));
    }
}
