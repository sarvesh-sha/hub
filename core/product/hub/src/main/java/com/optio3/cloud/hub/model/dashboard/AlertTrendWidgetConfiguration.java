/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.dashboard.enums.AlertTrendFrequency;

@JsonTypeName("AlertTrendWidgetConfiguration")
public class AlertTrendWidgetConfiguration extends AlertWidgetConfiguration
{
    public AlertTrendFrequency frequency;
}