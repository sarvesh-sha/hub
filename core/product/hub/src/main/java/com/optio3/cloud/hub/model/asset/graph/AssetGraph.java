/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset.graph;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.model.tags.TagsJoin;
import com.optio3.cloud.hub.model.tags.TagsJoinQuery;
import com.optio3.cloud.hub.model.tags.TagsJoinTerm;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceTypedValue;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.Base64EncodedValue;
import com.optio3.util.CollectionUtils;
import com.optio3.util.IdGenerator;

public class AssetGraph
{
    public static class Analyzed
    {
        public final AssetGraph                           graph;
        public final int                                  tupleWidth;
        public final Map<String, AssetGraphNode.Analyzed> lookup = Maps.newHashMap();

        Analyzed(AssetGraph graph)
        {
            this.graph = graph;

            List<AssetGraphNode> nodes = graph.nodes;
            tupleWidth = nodes.size();

            for (int pos = 0; pos < tupleWidth; pos++)
            {
                AssetGraphNode node = nodes.get(pos);
                lookup.put(node.id, new AssetGraphNode.Analyzed(node, pos));
            }

            for (AssetGraphTransform transform : graph.transforms)
            {
                AssetGraphNode.Analyzed nodeInput  = lookup.get(transform.inputId);
                AssetGraphNode.Analyzed nodeOutput = lookup.get(transform.outputId);

                if (nodeInput != null && nodeOutput != null)
                {
                    CollectionUtils.addIfMissing(nodeInput.children, nodeOutput);
                    CollectionUtils.addIfMissing(nodeOutput.parents, nodeInput);
                }
            }

            //
            // Fixup: only terminal nodes can allow multiple values.
            //
            for (AssetGraphNode.Analyzed node : lookup.values())
            {
                if (!node.children.isEmpty())
                {
                    node.node.allowMultiple = false;
                }
            }
        }

        public AssetGraphNode.Analyzed lookupNode(String nodeId)
        {
            return lookup.get(nodeId);
        }

        public List<AssetGraphNode.Analyzed> extractTreeStructure()
        {
            List<AssetGraphNode.Analyzed> tree = null;

            for (AssetGraphNode.Analyzed node : lookup.values())
            {
                if (node.isRoot())
                {
                    if (tree != null)
                    {
                        // More than one root, not a tree.
                        return null;
                    }

                    tree = extractTreeStructure(Lists.newArrayList(), node);
                }
            }

            return tree;
        }

        private List<AssetGraphNode.Analyzed> extractTreeStructure(List<AssetGraphNode.Analyzed> lst,
                                                                   AssetGraphNode.Analyzed node)
        {
            if (lst.contains(node))
            {
                // Block loops.
                return null;
            }

            lst.add(node);

            boolean multipleChildren = false;

            for (var subNode : node.children)
            {
                // Recurse, but only if it's the first child.
                lst = multipleChildren ? null : extractTreeStructure(lst, subNode);
                if (lst == null)
                {
                    break;
                }

                multipleChildren = true;
            }

            return lst;
        }
    }

    public final List<AssetGraphNode>      nodes      = Lists.newArrayList();
    public final List<AssetGraphTransform> transforms = Lists.newArrayList();

    public static boolean isValid(AssetGraph graph)
    {
        return graph != null && !graph.nodes.isEmpty();
    }

    public static SharedAssetGraph findBySharedGraphId(SessionHolder sessionHolder,
                                                       String id)
    {
        SystemPreferenceRecord.Tree tree = SystemPreferenceRecord.getPreferencesTree(sessionHolder);
        SystemPreferenceRecord.Tree node = tree.getNode(SystemPreferenceTypedValue.SharedAssetGraphConfig.getPath(), false);

        if (node != null)
        {
            SystemPreferenceRecord rec_pref = node.values.get(id);
            if (rec_pref != null)
            {
                try
                {
                    return rec_pref.getTypedValue(SharedAssetGraph.class);
                }
                catch (Throwable t)
                {
                }
            }
        }

        return null;
    }

    public Analyzed analyze()
    {
        return new Analyzed(this);
    }

    public static List<SharedAssetGraph> splitAssetGraph(AssetGraph graph)
    {
        if (graph == null)
        {
            return null;
        }

        Analyzed analyzed = graph.analyze();

        List<SharedAssetGraph> graphs = Lists.newArrayList();

        for (AssetGraphNode.Analyzed root : analyzed.lookup.values())
        {
            if (!root.isRoot())
            {
                continue;
            }

            AssetGraph       partial     = new AssetGraph();
            SharedAssetGraph sharedGraph = new SharedAssetGraph();
            sharedGraph.id    = IdGenerator.newGuid();
            sharedGraph.name  = root.node.name;
            sharedGraph.graph = partial;

            graphs.add(sharedGraph);

            Set<String> nodeIds = Sets.newHashSet();
            for (AssetGraphNode.Analyzed node : analyzed.lookup.values())
            {
                AssetGraphNode.Analyzed nodeRoot = node;
                while (nodeRoot != null && !nodeRoot.isRoot())
                {
                    nodeRoot = CollectionUtils.firstElement(nodeRoot.parents);
                }

                if (nodeRoot == root)
                {
                    partial.nodes.add(node.node);
                    nodeIds.add(node.node.id);
                }
            }

            for (AssetGraphTransform transform : graph.transforms)
            {
                if (nodeIds.contains(transform.inputId))
                {
                    partial.transforms.add(transform);
                }
            }
        }

        return graphs;
    }

    public static AssetGraph fromTagsJoinQuery(TagsJoinQuery query)
    {
        AssetGraph assetGraph = new AssetGraph();

        for (TagsJoinTerm term : query.terms)
        {
            AssetGraphNode node = new AssetGraphNode();
            node.id = term.id;

            try
            {
                node.name = new String(new Base64EncodedValue(term.id).getValue());
            }
            catch (Throwable t)
            {
                // Not a Base64 encoded name, ignore.
            }

            node.condition     = term.conditions;
            node.optional      = false;
            node.allowMultiple = false;

            assetGraph.nodes.add(node);
        }

        for (TagsJoin join : query.joins)
        {
            AssetGraphTransformRelationship transform = new AssetGraphTransformRelationship();
            transform.inputId      = join.leftSide;
            transform.outputId     = join.rightSide;
            transform.relationship = join.relation;

            assetGraph.transforms.add(transform);
        }

        return assetGraph;
    }
}
