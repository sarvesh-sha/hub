/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.serialization.Reflection;

public class EngineTab
{
    public String name;

    public final List<List<EngineBlock>> blockChains = Lists.newArrayList();

    public <T extends EngineBlock> T findTopBlock(Class<T> clz)
    {
        for (List<EngineBlock> blockChain : blockChains)
        {
            T blockTyped = findTopBlock(blockChain, clz);
            if (blockTyped != null)
            {
                return blockTyped;
            }
        }

        return null;
    }

    public static <T extends EngineBlock> T findTopBlock(List<EngineBlock> blockChain,
                                                         Class<T> clz)
    {
        for (EngineBlock block : blockChain)
        {
            T blockTyped = Reflection.as(block, clz);
            if (blockTyped != null)
            {
                return blockTyped;
            }
        }

        return null;
    }
}
