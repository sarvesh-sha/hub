/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.panes.configuration;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphBinding;

@JsonTypeName("PaneFieldConfigurationPathMap")
public class PaneFieldConfigurationPathMap extends PaneFieldConfiguration
{
    public AssetGraphBinding locationInput;
}
