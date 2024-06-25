/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = AggregationTableWidgetConfiguration.class),
                @JsonSubTypes.Type(value = AggregationTrendWidgetConfiguration.class),
                @JsonSubTypes.Type(value = AggregationWidgetConfiguration.class),
                @JsonSubTypes.Type(value = AlertTableWidgetConfiguration.class),
                @JsonSubTypes.Type(value = AlertWidgetConfiguration.class),
                @JsonSubTypes.Type(value = AssetGraphSelectorWidgetConfiguration.class),
                @JsonSubTypes.Type(value = ControlPointWidgetConfiguration.class),
                @JsonSubTypes.Type(value = DeviceWidgetConfiguration.class),
                @JsonSubTypes.Type(value = ImageWidgetConfiguration.class),
                @JsonSubTypes.Type(value = GroupingWidgetConfiguration.class),
                @JsonSubTypes.Type(value = TextWidgetConfiguration.class),
                @JsonSubTypes.Type(value = TimeSeriesWidgetConfiguration.class) })
public abstract class WidgetConfiguration
{
    public String id;

    public int size;

    public String name;

    public String description;

    public List<String> locations;

    public int refreshRateInSeconds;

    public boolean manualFontScaling;

    public double fontMultiplier;
    
    public WidgetToolbarBehavior toolbarBehavior;
}
