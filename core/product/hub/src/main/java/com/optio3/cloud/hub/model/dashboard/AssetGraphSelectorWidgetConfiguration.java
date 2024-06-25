/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.commons.lang3.StringUtils;

@JsonTypeName("AssetGraphSelectorWidgetConfiguration")
public class AssetGraphSelectorWidgetConfiguration extends WidgetConfiguration
{
    public String selectorId;

    // TODO: UPGRADE PATCH: Legacy fixup to remove field
    public void setGraphId(String graphId)
    {
        if (!StringUtils.isEmpty(graphId))
        {
            this.selectorId = graphId;
        }
    }
}
