/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.hub.engine.EngineExecutionContext;
import com.optio3.cloud.hub.engine.EngineExecutionStack;
import com.optio3.cloud.hub.engine.core.value.EngineValueLookupTable;
import com.optio3.cloud.hub.model.LookupEntry;
import com.optio3.util.BoxingUtils;

@JsonTypeName("EngineLiteralLookupTable")
public class EngineLiteralLookupTable extends EngineLiteralFromCore<EngineValueLookupTable>
{
    public final List<LookupEntry> entries = Lists.newArrayList();

    private final Supplier<Map<String, String>> m_mapCaseSensitive = Suppliers.memoize(() ->
                                                                                       {
                                                                                           Map<String, String> map = Maps.newHashMap();

                                                                                           for (LookupEntry entry : entries)
                                                                                           {
                                                                                               if (entry.key != null)
                                                                                               {
                                                                                                   if (entry.caseSensitive)
                                                                                                   {
                                                                                                       map.put(entry.key, BoxingUtils.get(entry.value, ""));
                                                                                                   }
                                                                                               }
                                                                                           }

                                                                                           return map;
                                                                                       });

    private final Supplier<Map<String, String>> m_mapCaseInsensitive = Suppliers.memoize(() ->
                                                                                         {
                                                                                             Map<String, String> map = Maps.newHashMap();

                                                                                             for (LookupEntry entry : entries)
                                                                                             {
                                                                                                 if (entry.key != null)
                                                                                                 {
                                                                                                     if (!entry.caseSensitive)
                                                                                                     {
                                                                                                         map.put(entry.key.toLowerCase(), BoxingUtils.get(entry.value, ""));
                                                                                                     }
                                                                                                 }
                                                                                             }

                                                                                             return map;
                                                                                         });

    //--//

    public EngineLiteralLookupTable()
    {
        super(EngineValueLookupTable.class);
    }

    @Override
    public void advanceExecution(EngineExecutionContext<?, ?> ctx,
                                 EngineExecutionStack stack)
    {
        ctx.popBlock(EngineValueLookupTable.create(m_mapCaseSensitive.get(), m_mapCaseInsensitive.get()));
    }
}
