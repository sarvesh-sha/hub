/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset.graph;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.optio3.cloud.model.BasePaginatedResponse;
import com.optio3.util.CollectionUtils;

public class AssetGraphResponse extends BasePaginatedResponse<AssetGraphResponse.Resolved>
{
    public final Map<String, AssetGraphResponseError> errors = Maps.newHashMap();

    public static class Resolved
    {
        @JsonIgnore
        public final AssetGraph.Analyzed graph;

        public final String[][] tuple;

        public Resolved()
        {
            this(null);
        }

        public Resolved(AssetGraph.Analyzed graph)
        {
            this.graph = graph;
            tuple      = new String[graph.tupleWidth][];
        }
    }

    public void resolve(AssetGraph.Analyzed graph,
                        Collection<String[]> tuples)
    {
        List<AssetGraphNode.Analyzed> tree = graph.extractTreeStructure();
        if (tree != null)
        {
            // Simple case, all the nodes are arranged in a tree.
            collect(graph, tuples, tree, 0);
        }
        else
        {
            List<AssetGraphNode.Analyzed> queue = Lists.newArrayList();

            //
            // First pass, only add root nodes.
            //
            for (AssetGraphNode.Analyzed nodeAnalyzed : graph.lookup.values())
            {
                if (nodeAnalyzed.isRoot())
                {
                    queue.add(nodeAnalyzed);
                }
            }

            //
            // Second pass, only add single-value nodes.
            //
            for (AssetGraphNode.Analyzed nodeAnalyzed : graph.lookup.values())
            {
                if (!nodeAnalyzed.node.allowMultiple)
                {
                    CollectionUtils.addIfMissing(queue, nodeAnalyzed);
                }
            }

            // Third pass, add all the nodes.
            for (AssetGraphNode.Analyzed nodeAnalyzed : graph.lookup.values())
            {
                CollectionUtils.addIfMissing(queue, nodeAnalyzed);
            }

            collect(graph, tuples, queue, 0);
        }
    }

    private void collect(AssetGraph.Analyzed graph,
                         Collection<String[]> tuples,
                         List<AssetGraphNode.Analyzed> nodes,
                         int nodeIndex)
    {
        int numTuples = tuples.size();
        if (numTuples == 0)
        {
            return;
        }

        if (numTuples > 1 && nodeIndex < nodes.size())
        {
            AssetGraphNode.Analyzed node = findNode(nodes, nodeIndex);
            if (!node.node.allowMultiple)
            {
                Multimap<String, String[]> cluster = distributeBasedOnNode(tuples, node);

                for (String sysId : cluster.keySet())
                {
                    Collection<String[]> subTuples = cluster.get(sysId);
                    collect(graph, subTuples, nodes, nodeIndex + 1);
                }
            }
            else
            {
                collect(graph, tuples, nodes, nodeIndex + 1);
            }
        }
        else
        {
            var      result     = new Resolved(graph);
            String[] emptyArray = new String[0];

            if (numTuples == 1)
            {
                String[] tuple = CollectionUtils.firstElement(tuples);

                for (int i = 0; i < graph.tupleWidth; i++)
                {
                    AssetGraphNode.Analyzed nodeAnalyzed = findNode(nodes, i);

                    String sysId = tuple[i];
                    if (sysId != null)
                    {
                        if (result.tuple[i] != null)
                        {
                            continue;
                        }

                        result.tuple[i] = new String[] { sysId };
                    }
                    else
                    {
                        result.tuple[i] = emptyArray;

                        if (!nodeAnalyzed.node.optional)
                        {
                            var optionalAncestor = nodeAnalyzed.findOptionalAncestor();
                            if (optionalAncestor == null)
                            {
                                errors.put(nodeAnalyzed.node.id, AssetGraphResponseError.RequiredNodeNoAsset);
                            }
                            else
                            {
                                var queue = Lists.newArrayList(optionalAncestor);
                                while (queue.size() > 0)
                                {
                                    var node = queue.remove(0);
                                    result.tuple[node.index] = emptyArray;
                                    queue.addAll(node.children);
                                }
                            }
                        }
                    }
                }
            }
            else
            {
                Multimap<Integer, String> sysIds = HashMultimap.create();

                for (int i = 0; i < graph.tupleWidth; i++)
                {
                    AssetGraphNode.Analyzed nodeAnalyzed = findNode(nodes, i);

                    for (String[] tuple : tuples)
                    {
                        String sysId = tuple[i];
                        if (sysId != null)
                        {
                            sysIds.put(i, sysId);
                        }
                        else if (!nodeAnalyzed.node.optional)
                        {
                            var optionalAncestor = nodeAnalyzed.findOptionalAncestor();
                            if (optionalAncestor == null)
                            {
                                errors.put(nodeAnalyzed.node.id, AssetGraphResponseError.RequiredNodeNoAsset);
                            }
                            else
                            {
                                var queue = Lists.newArrayList(optionalAncestor);
                                while (queue.size() > 0)
                                {
                                    var node = queue.remove(0);
                                    sysId = tuple[node.index];
                                    if (sysId != null)
                                    {
                                        sysIds.remove(node.index, sysId);
                                    }
                                    queue.addAll(node.children);
                                }
                            }
                        }
                    }
                }

                for (int i = 0; i < graph.tupleWidth; i++)
                {
                    AssetGraphNode.Analyzed nodeAnalyzed = findNode(nodes, i);
                    Collection<String>      unique       = sysIds.get(i);
                    if (unique.size() > 1 && !nodeAnalyzed.node.allowMultiple)
                    {
                        errors.put(nodeAnalyzed.node.id, AssetGraphResponseError.MultipleMatches);
                    }

                    result.tuple[i] = unique.toArray(emptyArray);
                }
            }

            results.add(result);
        }
    }

    private AssetGraphNode.Analyzed findNode(List<AssetGraphNode.Analyzed> nodes,
                                             int index)
    {
        return CollectionUtils.findFirst(nodes, (n) -> n.index == index);
    }

    private Multimap<String, String[]> distributeBasedOnNode(Collection<String[]> results,
                                                             AssetGraphNode.Analyzed node)
    {
        Multimap<String, String[]> cluster = ArrayListMultimap.create();

        for (String[] tuple : results)
        {
            String sysId = tuple[node.index];
            cluster.put(sysId, tuple);
        }

        return cluster;
    }
}
