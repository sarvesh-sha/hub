/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.model.dashboard.enums.HorizontalAlignment;
import com.optio3.cloud.hub.model.dashboard.enums.VerticalAlignment;

public class BrandingConfiguration
{
    public String primaryColor;

    public String secondaryColor;

    public String text;

    public String logoBase64;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setLogoPlacement(LogoPlacement logoPlacement)
    {
        HubApplication.reportPatchCall(logoPlacement);

        switch (logoPlacement)
        {
            case Left:
                horizontalPlacement = HorizontalAlignment.Left;
                break;

            case Right:
                horizontalPlacement = HorizontalAlignment.Right;
                break;

            default:
                horizontalPlacement = HorizontalAlignment.Center;
                break;
        }
    }

    public HorizontalAlignment horizontalPlacement = HorizontalAlignment.Center;

    public VerticalAlignment verticalPlacement = VerticalAlignment.Middle;
}
