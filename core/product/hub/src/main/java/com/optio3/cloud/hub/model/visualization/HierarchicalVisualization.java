/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.visualization;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphNode;
import com.optio3.cloud.hub.model.asset.graph.SharedAssetGraph;
import com.optio3.cloud.hub.model.asset.graph.VirtualAssetGraphNode;
import com.optio3.cloud.hub.model.asset.graph.VirtualAssetGraphNodeType;
import com.optio3.util.CollectionUtils;

public class HierarchicalVisualization
{
    private final List<HierarchicalVisualizationBinding> m_bindings   = Lists.newArrayList();
    public final  List<VirtualAssetGraphNode>            virtualNodes = Lists.newArrayList();
    public        InteractionBehavior                    interactionBehavior;

    //--//

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setColors(ColorOldConfiguration colors)
    {
        if (colors != null)
        {
            HubApplication.reportPatchCall(colors);

            this.setColorSettings(colors.upgrade());
        }
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field.
    public void setHierarchy(JsonNode node)
    {
        // Removed
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field.
    public void setRange(RangeSelection range)
    {
        // Removed
    }

    private List<VirtualColumn> m_columns;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field.
    public void setColumns(List<VirtualColumn> columns)
    {
        m_columns = columns;
    }

    public void fixupVirtualNodes(TimeSeriesGraphConfiguration queryGraph)
    {
        if (m_columns != null)
        {
            SharedAssetGraph sharedGraph = CollectionUtils.firstElement(queryGraph.sharedGraphs);
            if (sharedGraph != null)
            {
                List<AssetGraphNode> nodes    = sharedGraph.graph.nodes;
                AssetGraphNode       lastNode = CollectionUtils.lastElement(nodes);
                if (lastNode != null)
                {
                    for (VirtualColumn column : m_columns)
                    {
                        if (column.selected && nodes.size() > column.index)
                        {
                            VirtualAssetGraphNode virtualNode = new VirtualAssetGraphNode();
                            virtualNode.nodeId    = nodes.get(column.index).id;
                            virtualNode.ascending = column.ascending;
                            virtualNode.collapsed = column.collapsed;

                            switch (column.property)
                            {
                                case EquipmentClass:
                                    virtualNode.type = VirtualAssetGraphNodeType.EquipmentClass;
                                    break;

                                case Location:
                                    virtualNode.type = VirtualAssetGraphNodeType.Location;
                                    break;

                                case Name:
                                    virtualNode.type = VirtualAssetGraphNodeType.Name;
                                    break;

                                case PointClass:
                                    virtualNode.type = VirtualAssetGraphNodeType.PointClass;
                                    break;
                            }

                            virtualNodes.add(virtualNode);
                        }
                    }

                    this.setLeafNodeId(lastNode.id);
                }
            }

            m_columns = null;
        }
    }

    //--//

    private String                                 m_leafNodeId;
    private HierarchicalVisualizationConfiguration m_options;
    private ColorConfiguration                     m_colorSettings;

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setLeafNodeId(String leafNodeId)
    {
        m_leafNodeId = leafNodeId;
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setOptions(HierarchicalVisualizationConfiguration options)
    {
        m_options = options;
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setColorSettings(ColorConfiguration colorSettings)
    {
        m_colorSettings = colorSettings;
    }

    public List<HierarchicalVisualizationBinding> getBindings()
    {
        if (m_leafNodeId != null)
        {
            HierarchicalVisualizationBinding binding = new HierarchicalVisualizationBinding();
            binding.leafNodeId = m_leafNodeId;
            m_leafNodeId       = null;

            if (m_options != null)
            {
                binding.options = m_options;
            }

            if (m_colorSettings != null)
            {
                binding.color = m_colorSettings;
            }

            m_bindings.add(binding);
        }

        return m_bindings;
    }
}
