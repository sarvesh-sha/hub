/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.model.alert.AlertType;

@JsonSubTypes({ @JsonSubTypes.Type(value = AlertFeedWidgetConfiguration.class),
                @JsonSubTypes.Type(value = AlertMapWidgetConfiguration.class),
                @JsonSubTypes.Type(value = AlertSummaryWidgetConfiguration.class),
                @JsonSubTypes.Type(value = AlertTrendWidgetConfiguration.class) })
public abstract class AlertWidgetConfiguration extends WidgetConfiguration
{
    public List<AlertType> alertTypes;
}
