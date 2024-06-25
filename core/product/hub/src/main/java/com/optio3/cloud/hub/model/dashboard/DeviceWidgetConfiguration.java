/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({ @JsonSubTypes.Type(value = DeviceSummaryWidgetConfiguration.class) })
public abstract class DeviceWidgetConfiguration extends WidgetConfiguration
{
    public List<String> categories;
    public List<String> states;
}
