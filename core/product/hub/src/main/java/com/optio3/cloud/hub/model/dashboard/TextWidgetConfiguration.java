/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.dashboard.enums.HorizontalAlignment;

@JsonTypeName("TextWidgetConfiguration")
public class TextWidgetConfiguration extends WidgetConfiguration
{
    public String              text;
    public String              color;
    public HorizontalAlignment alignment;
    public boolean             preventWrapping;
}
