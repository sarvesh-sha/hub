/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphBinding;
import com.optio3.cloud.hub.model.dashboard.enums.ControlPointDisplayType;
import com.optio3.cloud.hub.model.dashboard.enums.HorizontalAlignment;
import com.optio3.cloud.hub.model.visualization.ColorConfiguration;
import com.optio3.protocol.model.EngineeringUnitsFactors;

@JsonTypeName("ControlPointWidgetConfiguration")
public class ControlPointWidgetConfiguration extends WidgetConfiguration
{
    public String            pointId;
    public AssetGraphBinding pointInput;

    public boolean                 nameEnabled;
    public ControlPointDisplayType nameDisplay;
    public HorizontalAlignment     nameAlignment;

    public boolean                 valueEnabled;
    public EngineeringUnitsFactors valueUnits;
    public boolean                 valueUnitsEnabled;
    public int                     valuePrecision;
    public HorizontalAlignment     valueAlignment;

    public boolean             timestampEnabled;
    public String              timestampFormat;
    public HorizontalAlignment timestampAlignment;

    public ColorConfiguration color;
}
