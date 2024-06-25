/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.panes.configuration;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = PaneFieldConfigurationAggregatedValue.class),
                @JsonSubTypes.Type(value = PaneFieldConfigurationAlertCount.class),
                @JsonSubTypes.Type(value = PaneFieldConfigurationAlertFeed.class),
                @JsonSubTypes.Type(value = PaneFieldConfigurationChart.class),
                @JsonSubTypes.Type(value = PaneFieldConfigurationCurrentValue.class),
                @JsonSubTypes.Type(value = PaneFieldConfigurationPathMap.class) })
public abstract class PaneFieldConfiguration
{
    public String label;
}
