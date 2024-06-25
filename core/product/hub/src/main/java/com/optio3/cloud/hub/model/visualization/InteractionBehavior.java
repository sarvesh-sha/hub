package com.optio3.cloud.hub.model.visualization;

import com.optio3.cloud.annotation.Optio3Sanitize;
import com.optio3.cloud.hub.model.dashboard.panes.configuration.PaneConfiguration;

public class InteractionBehavior
{
    public InteractionBehaviorType type;

    @Optio3Sanitize(handler = PaneConfiguration.Sanitizer.class)
    public String paneConfigId;
}
