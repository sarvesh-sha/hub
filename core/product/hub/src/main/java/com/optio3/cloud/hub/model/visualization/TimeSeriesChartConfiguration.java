/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.visualization;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.asset.graph.AssetGraph;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphBinding;
import com.optio3.cloud.hub.model.asset.graph.SharedAssetGraph;
import com.optio3.cloud.hub.model.tags.TagsJoinQuery;
import com.optio3.util.IdGenerator;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("TimeSeriesChartConfiguration")
public class TimeSeriesChartConfiguration extends TimeSeriesConfigurationBase
{
    public  TimeSeriesChartType                     type;
    public  TimeSeriesDisplayConfiguration          display;
    public  List<TimeSeriesPanelConfiguration>      panels;
    private HierarchicalVisualization               m_hierarchy;
    private String                                  m_palette;
    public  TimeSeriesTooltipConfiguration          tooltip;
    public  List<TimeSeriesAnnotationConfiguration> annotations;

    private List<TimeSeriesSourceConfiguration> m_dataSources;

    public void setDataSources(List<TimeSeriesSourceConfiguration> sources)
    {
        m_dataSources = sources;
    }

    public List<TimeSeriesSourceConfiguration> getDataSources()
    {
        if (m_oldSources != null)
        {
            m_dataSources = m_oldSources;
            m_oldSources  = null;

            for (TimeSeriesSourceConfiguration source : m_dataSources)
            {
                fixupBinding(source.pointBinding);
            }
        }

        return m_dataSources;
    }

    private List<TimeSeriesSourceConfiguration> m_oldSources;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field.
    public void setSources(List<TimeSeriesSourceConfiguration> sources)
    {
        m_oldSources = sources;
    }

    private ScatterPlot m_scatterPlot;

    public void setScatterPlot(ScatterPlot scatterPlot)
    {
        m_scatterPlot = scatterPlot;
    }

    public ScatterPlot getScatterPlot()
    {
        if (m_oldScatter != null)
        {
            m_scatterPlot = m_oldScatter;
            m_oldScatter  = null;

            if (m_scatterPlot.sourceTuples != null)
            {
                for (ScatterPlotSourceTuple tuple : m_scatterPlot.sourceTuples)
                {
                    fixupScatterSource(tuple.sourceX);
                    fixupScatterSource(tuple.sourceY);
                    fixupScatterSource(tuple.sourceZ);
                }
            }
        }

        return m_scatterPlot;
    }

    private ScatterPlot m_oldScatter;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field.
    public void setScatter(ScatterPlot scatter)
    {
        m_oldScatter = scatter;
    }

    private void fixupScatterSource(ScatterPlotSource source)
    {
        if (source != null)
        {
            fixupBinding(source.binding);
        }
    }

    private void fixupBinding(AssetGraphBinding binding)
    {
        TimeSeriesGraphConfiguration graph = getGraph();
        if (binding != null && graph != null)
        {
            for (SharedAssetGraph sharedGraph : graph.sharedGraphs)
            {
                AssetGraph.Analyzed analyzed = sharedGraph.graph.analyze();
                if (analyzed.lookupNode(binding.nodeId) != null)
                {
                    binding.graphId = sharedGraph.id;
                }
            }
        }
    }

    public void setPalette(String palette)
    {
        if (StringUtils.equals(palette, "Default"))
        {
            palette = null;
        }

        m_palette = palette;
    }

    public String getPalette()
    {
        return m_palette;
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field.
    public void setQuery(TagsJoinQuery query)
    {
        m_query = query;
    }

    private TagsJoinQuery m_query;

    private TimeSeriesGraphConfiguration m_graph;

    // TODO: UPGRADE PATCH: Legacy fixup from removed field: query
    public TimeSeriesGraphConfiguration getGraph()
    {
        performQueryFixup();

        return m_graph;
    }

    public void setGraph(TimeSeriesGraphConfiguration graph)
    {
        m_graph = graph;
    }

    // TODO: UPGRADE PATCH: Legacy fixup for modified HierarchicalVisualization field
    public void setHierarchy(HierarchicalVisualization hierarchy)
    {
        m_hierarchy = hierarchy;
    }

    public HierarchicalVisualization getHierarchy()
    {
        performQueryFixup();

        return m_hierarchy;
    }

    private void performQueryFixup()
    {
        if (m_query != null)
        {
            SharedAssetGraph sharedAssetGraph = new SharedAssetGraph();
            sharedAssetGraph.id    = IdGenerator.newGuid();
            sharedAssetGraph.name  = "Asset Graph";
            sharedAssetGraph.graph = AssetGraph.fromTagsJoinQuery(m_query);

            TimeSeriesGraphConfiguration graphConfig = new TimeSeriesGraphConfiguration();
            graphConfig.sharedGraphs.add(sharedAssetGraph);

            if (m_hierarchy != null)
            {
                m_hierarchy.fixupVirtualNodes(graphConfig);
            }

            m_query = null;
            m_graph = graphConfig;
        }
    }
}
