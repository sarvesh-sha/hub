/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.ControlPointsSelection;
import com.optio3.cloud.hub.model.asset.graph.AssetGraph;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphBinding;
import com.optio3.cloud.hub.model.dashboard.enums.AggregationGranularity;
import com.optio3.cloud.hub.model.dashboard.enums.AggregationLimit;
import com.optio3.cloud.hub.model.dashboard.enums.AggregationTypeId;
import com.optio3.cloud.hub.model.tags.TagsJoinQuery;
import com.optio3.cloud.hub.model.tags.TagsJoinTerm;
import com.optio3.cloud.hub.model.visualization.ColorConfiguration;
import com.optio3.cloud.hub.model.visualization.ColorSegment;
import com.optio3.cloud.hub.model.visualization.ToggleableNumericRange;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.util.CollectionUtils;

public class ControlPointsGroup
{
    public String                  name;
    public EngineeringUnitsFactors unitsFactors;
    public String                  unitsDisplay;
    public AggregationTypeId       aggregationType;
    public AggregationTypeId       groupAggregationType;
    public AggregationGranularity  granularity = AggregationGranularity.None;
    public AggregationLimit        limitMode   = AggregationLimit.None;
    public int                     limitValue;
    public int                     valuePrecision;
    public ControlPointsSelection  selections;
    public ColorConfiguration      colorConfig;
    public ToggleableNumericRange  range;

    //--//

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setColor(String color)
    {
        HubApplication.reportPatchCall(color);

        if (color != null)
        {
            ColorConfiguration colorConfig = new ColorConfiguration();
            ColorSegment       segment     = new ColorSegment();
            segment.color = color;
            colorConfig.segments.add(segment);
            this.colorConfig = colorConfig;
        }
    }

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setUnits(EngineeringUnits unit)
    {
        HubApplication.reportPatchCall(unit);

        this.unitsFactors = EngineeringUnitsFactors.get(unit);
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field.
    public void setQuery(TagsJoinQuery query)
    {
        m_query = query;
    }

    private TagsJoinQuery m_query;

    private AssetGraphBinding m_pointInput;

    // TODO: UPGRADE PATCH: Legacy fixup for conversion from query to graph
    public AssetGraphBinding getPointInput()
    {
        performQueryFixup();

        return m_pointInput;
    }

    public void setPointInput(AssetGraphBinding pointInput)
    {
        m_pointInput = pointInput;
    }

    // TODO: UPGRADE PATCH: Legacy fixup for conversion from query to graph
    private AssetGraph m_graph;

    public AssetGraph getGraph()
    {
        performQueryFixup();

        return m_graph;
    }

    public void setGraph(AssetGraph graph)
    {
        m_graph = graph;
    }

    private void performQueryFixup()
    {
        if (m_query != null)
        {
            TagsJoinTerm lastTerm = CollectionUtils.lastElement(m_query.terms);
            if (lastTerm != null)
            {
                m_graph = AssetGraph.fromTagsJoinQuery(m_query);

                m_pointInput         = new AssetGraphBinding();
                m_pointInput.graphId = null;
                m_pointInput.nodeId  = lastTerm.id;
            }

            m_query = null;
        }
    }
}

