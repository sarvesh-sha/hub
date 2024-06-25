/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.visualization;

public class TimeSeriesDisplayConfiguration
{
    public String  title;
    public int     size;
    public boolean fillArea;
    public boolean hideDecimation;
    public boolean automaticAggregation;
    public int     panelSpacing;
    public boolean showAlerts = true;
    public boolean hideSources;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setZoomable(boolean zoomable)
    {
        // removed
    }
}
