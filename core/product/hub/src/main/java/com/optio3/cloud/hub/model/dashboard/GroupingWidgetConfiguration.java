/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("GroupingWidgetConfiguration")
public class GroupingWidgetConfiguration extends WidgetConfiguration
{
    public int                     numCols;
    public int                     numRows;
    public List<WidgetComposition> widgets;
}
