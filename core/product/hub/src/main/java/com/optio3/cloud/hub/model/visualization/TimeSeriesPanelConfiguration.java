/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.visualization;

import com.optio3.cloud.hub.HubApplication;

public class TimeSeriesPanelConfiguration
{
    public TimeSeriesAxisConfiguration xAxis;
    public TimeSeriesAxisConfiguration leftAxis;
    public TimeSeriesAxisConfiguration rightAxis;
    public ColorConfiguration          colorSettings;

    //--//

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setHideBottomBorder(boolean hide)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setHideBottomAxis(boolean hide)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setBorderColor(String color)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setGridColor(String color)
    {
    }

    //--//

    // TODO: UPGRADE PATCH: Legacy fixup for field type change.
    public void setColors(ColorOldConfiguration colors)
    {
        if (colors != null)
        {
            HubApplication.reportPatchCall(colors);

            this.colorSettings = colors.upgrade();
        }
    }
}
