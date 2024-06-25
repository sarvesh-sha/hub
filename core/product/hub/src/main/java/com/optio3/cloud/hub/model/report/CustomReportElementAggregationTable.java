/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.hub.model.dashboard.AggregationNodeBinding;
import com.optio3.cloud.hub.model.dashboard.ControlPointsGroup;
import com.optio3.cloud.hub.model.dashboard.enums.ControlPointDisplayType;
import com.optio3.cloud.hub.model.visualization.HierarchicalVisualizationType;
import com.optio3.cloud.model.SortCriteria;

@JsonTypeName("CustomReportElementAggregationTable")
public class CustomReportElementAggregationTable extends CustomReportElement
{
    public       String                        label;
    public       List<ControlPointsGroup>      groups;
    public final List<AggregationNodeBinding>  columns                 = Lists.newArrayList();
    public       String                        graphId;
    public       boolean                       isolateGroupRanges      = false;
    public       ControlPointDisplayType       controlPointDisplayType = ControlPointDisplayType.NameOnly;
    public       HierarchicalVisualizationType visualizationMode       = HierarchicalVisualizationType.TABLE;
    public       SortCriteria                  initialSort;
}
