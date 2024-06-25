/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard.panes;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = PaneFieldAggregatedValue.class),
                @JsonSubTypes.Type(value = PaneFieldAlertCount.class),
                @JsonSubTypes.Type(value = PaneFieldAlertFeed.class),
                @JsonSubTypes.Type(value = PaneFieldChart.class),
                @JsonSubTypes.Type(value = PaneFieldCurrentValue.class),
                @JsonSubTypes.Type(value = PaneFieldGauge.class),
                @JsonSubTypes.Type(value = PaneFieldNumber.class),
                @JsonSubTypes.Type(value = PaneFieldPathMap.class),
                @JsonSubTypes.Type(value = PaneFieldString.class) })
public abstract class PaneField
{
    public String label;
}
