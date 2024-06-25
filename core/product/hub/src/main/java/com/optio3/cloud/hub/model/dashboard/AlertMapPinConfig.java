/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import java.util.List;

import com.optio3.cloud.hub.model.dashboard.enums.AlertMapPinColorMode;
import com.optio3.cloud.hub.model.dashboard.enums.AlertMapPinDataSource;
import com.optio3.cloud.hub.model.visualization.ColorSegment;
import com.optio3.cloud.hub.model.visualization.MapPinIcon;

public class AlertMapPinConfig
{
    public MapPinIcon                  pinIcon;
    public int                         pinSize;
    public AlertMapPinColorMode        colorMode;
    public AlertMapPinDataSource       dataSource;
    public String                      staticColor;
    public List<ColorSegment>          countColors;
    public List<AlertMapSeverityColor> severityColors;
}
