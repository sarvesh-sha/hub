/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.asset.graph.AssetGraph;
import com.optio3.cloud.hub.model.dashboard.enums.ControlPointDisplayType;
import com.optio3.cloud.hub.model.schedule.FilterableTimeRange;
import com.optio3.cloud.hub.model.visualization.HierarchicalVisualizationType;
import com.optio3.cloud.hub.model.visualization.InteractionBehavior;
import com.optio3.cloud.hub.model.visualization.InteractionBehaviorType;
import com.optio3.cloud.hub.model.visualization.RangeSelection;
import com.optio3.util.CollectionUtils;

@JsonTypeName("AggregationTableWidgetConfiguration")
public class AggregationTableWidgetConfiguration extends WidgetConfiguration
{
    public       List<ControlPointsGroup>     groups;
    public final List<AggregationNodeBinding> columns = Lists.newArrayList();

    public  AssetGraph                    graph;
    public  List<FilterableTimeRange>     filterableRanges;
    public  ControlPointDisplayType       controlPointDisplayType = ControlPointDisplayType.NameOnly;
    public  HierarchicalVisualizationType visualizationMode       = HierarchicalVisualizationType.TABLE;
    public  boolean                       isolateGroupRanges      = false;
    public  boolean                       visualizationLegend     = false;
    public  boolean                       visualizationRanges     = false;
    private InteractionBehavior           m_clickBehavior;

    public InteractionBehavior getClickBehavior()
    {
        clickBehaviorFixup();

        return m_clickBehavior;
    }

    public void setClickBehavior(InteractionBehavior clickBehavior)
    {
        m_clickBehavior = clickBehavior;
    }

    private String                    m_paneConfigId;
    private WidgetInteractionBehavior m_interactionBehavior;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setPaneConfigId(String paneConfigId)
    {
        m_paneConfigId = paneConfigId;
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setInteractionBehavior(WidgetInteractionBehavior behavior)
    {
        m_interactionBehavior = behavior;
    }

    // TODO: UPGRADE PATCH: This setter converts it to the correct format: of FilterableTimeRange
    public void setRanges(List<RangeSelection> ranges)
    {
        HubApplication.reportPatchCall(ranges);

        this.filterableRanges = CollectionUtils.transformToListNoNulls(ranges, (range) ->
        {
            FilterableTimeRange newFilterable = new FilterableTimeRange();
            newFilterable.range = range;
            return newFilterable;
        });
    }

    private void clickBehaviorFixup()
    {
        if (this.m_clickBehavior == null)
        {
            WidgetInteractionBehavior oldBehavior = m_interactionBehavior;
            if (oldBehavior == null)
            {
                if (m_paneConfigId != null)
                {
                    oldBehavior = WidgetInteractionBehavior.Pane;
                }
                else
                {
                    oldBehavior = WidgetInteractionBehavior.Standard;
                }
            }

            this.m_clickBehavior = new InteractionBehavior();
            switch (oldBehavior)
            {
                case DataExplorer:
                    m_clickBehavior.type = InteractionBehaviorType.NavigateDataExplorer;
                    break;

                case Pane:
                    m_clickBehavior.type = InteractionBehaviorType.Pane;
                    m_clickBehavior.paneConfigId = m_paneConfigId;
                    break;

                default:
                    m_clickBehavior.type = InteractionBehaviorType.Standard;
                    break;
            }
        }
    }
}
