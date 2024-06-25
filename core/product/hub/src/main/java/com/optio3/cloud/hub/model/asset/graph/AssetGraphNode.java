/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset.graph;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.model.tags.TagsCondition;

public class AssetGraphNode
{
    public static class Analyzed
    {
        public final AssetGraphNode node;
        public final int            index;

        public final List<AssetGraphNode.Analyzed> children = Lists.newArrayList();
        public final List<AssetGraphNode.Analyzed> parents  = Lists.newArrayList();

        public Analyzed(AssetGraphNode node,
                        int index)
        {
            this.node  = node;
            this.index = index;
        }

        public boolean isRoot()
        {
            return parents.isEmpty();
        }

        public Analyzed findOptionalAncestor()
        {
            for (Analyzed parent : parents)
            {
                if (parent.node.optional)
                {
                    return parent;
                }

                Analyzed ancestor = parent.findOptionalAncestor();
                if (ancestor != null)
                {
                    return ancestor;
                }
            }

            return null;
        }

        public boolean isOptional()
        {
            return node.optional || findOptionalAncestor() != null;
        }
    }

    public String id;

    public String name;

    public boolean optional;

    public boolean allowMultiple;

    public TagsCondition condition;
}
