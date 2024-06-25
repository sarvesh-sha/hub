/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.visualization;

import com.optio3.cloud.model.IEnumDescription;

public enum HierarchicalVisualizationType implements IEnumDescription
{
    HEATMAP("Heatmap", null),
    LINE("Line", null),
    TABLE("Table", null),
    TABLE_WITH_BAR("Table With Bars", null),
    BUBBLEMAP("Bubble Map", null),
    TREEMAP("Tree Map", null),
    SUNBURST("Sunburst", null),
    PIEBURST("Pieburst", null),
    DONUT("Donut", null),
    PIE("Pie", null);

    private final String m_displayName;
    private final String m_description;

    HierarchicalVisualizationType(String displayName,
                                  String description)
    {
        m_displayName = displayName;
        m_description = description;
    }

    @Override
    public String getDisplayName()
    {
        return this.m_displayName;
    }

    @Override
    public String getDescription()
    {
        return this.m_description;
    }
}
