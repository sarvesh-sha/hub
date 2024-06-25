/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.visualization;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.asset.graph.AssetGraph;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphBinding;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphContext;
import com.optio3.cloud.hub.model.asset.graph.SharedAssetGraph;
import org.apache.commons.lang3.StringUtils;

public class TimeSeriesGraphConfiguration
{
    public final List<SharedAssetGraph>  sharedGraphs     = Lists.newArrayList();
    public final List<AssetGraphBinding> externalBindings = Lists.newArrayList();

    private       List<AssetGraphContext> m_contexts;
    private final List<AssetGraphContext> m_oldContexts = Lists.newArrayList();

    public void setContexts(List<AssetGraphContext> contexts)
    {
        m_contexts = contexts;
    }

    public List<AssetGraphContext> getContexts()
    {
        if (m_contexts == null)
        {
            m_contexts = Lists.newArrayList();

            for (AssetGraphContext context : m_oldContexts)
            {
                if (StringUtils.isEmpty(context.graphId))
                {
                    for (SharedAssetGraph sharedGraph : sharedGraphs)
                    {
                        AssetGraph.Analyzed analyzed = sharedGraph.graph.analyze();
                        if (analyzed.lookupNode(context.nodeId) != null)
                        {
                            context.graphId = sharedGraph.id;
                            m_contexts.add(context);
                            break;
                        }
                    }
                }
            }

            m_oldContexts.clear();
        }

        return m_contexts;
    }

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setGraphContext(AssetGraphContext graphContext)
    {
        HubApplication.reportPatchCall(graphContext);

        if (graphContext != null)
        {
            m_oldContexts.add(graphContext);
        }
    }

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setGraphContexts(List<AssetGraphContext> graphContexts)
    {
        HubApplication.reportPatchCall(graphContexts);

        if (graphContexts != null)
        {
            m_oldContexts.addAll(graphContexts);
        }
    }

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setGraph(AssetGraph graph)
    {
        HubApplication.reportPatchCall(graph);

        if (graph != null)
        {
            List<SharedAssetGraph> graphs = AssetGraph.splitAssetGraph(graph);

            sharedGraphs.addAll(graphs);
        }
    }

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setSharedGraphId(String sharedGraphId)
    {
        HubApplication.reportPatchCall(sharedGraphId);

        // Could look up old shared graph but not as a JSON fixup
    }
}
