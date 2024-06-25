/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.location.LocationType;
import com.optio3.cloud.hub.model.visualization.InteractionBehavior;
import com.optio3.cloud.hub.model.visualization.InteractionBehaviorType;

@JsonTypeName("AlertMapWidgetConfiguration")
public class AlertMapWidgetConfiguration extends AlertWidgetConfiguration
{
    public String          center;
    public AlertMapOptions options;

    public InteractionBehavior clickBehavior;

    public AlertMapPinConfig pin;

    public LocationType rollupType;

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setPaneConfigId(String paneConfigId)
    {
        if (paneConfigId != null)
        {
            clickBehavior              = new InteractionBehavior();
            clickBehavior.type         = InteractionBehaviorType.Pane;
            clickBehavior.paneConfigId = paneConfigId;
        }
    }
}
