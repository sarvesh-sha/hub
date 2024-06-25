/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.Maps;
import com.optio3.cloud.hub.engine.core.block.EngineProcedureDeclaration;
import com.optio3.cloud.hub.engine.core.block.EngineThread;
import com.optio3.cloud.persistence.ModelSanitizerContext;
import com.optio3.cloud.persistence.ModelSanitizerHandler;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.Reflection;
import org.apache.commons.lang3.StringUtils;

public class EngineExecutionProgram<D extends EngineDefinitionDetails>
{
    public final D                                       definition;
    public final EngineThread                            mainThread;
    public final Map<String, EngineBlock>                blockLookup;
    public final Map<String, EngineProcedureDeclaration> functionLookup;

    //--//

    public EngineExecutionProgram(SessionHolder sessionHolder,
                                  D details)
    {
        AtomicReference<EngineThread>           mainThread     = new AtomicReference<>();
        Map<String, EngineBlock>                blockLookup    = Maps.newHashMap();
        Map<String, EngineProcedureDeclaration> functionLookup = Maps.newHashMap();

        ModelSanitizerContext ctx = new ModelSanitizerContext.Simple(sessionHolder)
        {
            @Override
            protected ModelSanitizerHandler.Target processInner(Object obj,
                                                                ModelSanitizerHandler handler)
            {
                EngineBlock block = Reflection.as(obj, EngineBlock.class);
                if (block != null)
                {
                    if (StringUtils.isNotEmpty(block.id))
                    {
                        blockLookup.put(block.id, block);
                    }

                    EngineProcedureDeclaration func = Reflection.as(block, EngineProcedureDeclaration.class);
                    if (func != null)
                    {
                        functionLookup.put(func.functionId, func);
                    }

                    EngineThread thread = Reflection.as(block, EngineThread.class);
                    if (thread != null)
                    {
                        mainThread.set(thread);
                    }
                }

                return super.processInner(obj, handler);
            }
        };

        this.definition = ctx.processTyped(details);

        this.mainThread     = mainThread.get();
        this.blockLookup    = Collections.unmodifiableMap(blockLookup);
        this.functionLookup = Collections.unmodifiableMap(functionLookup);
    }
}
