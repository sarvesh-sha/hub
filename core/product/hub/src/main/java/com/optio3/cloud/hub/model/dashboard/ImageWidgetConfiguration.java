/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.dashboard;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("ImageWidgetConfiguration")
public class ImageWidgetConfiguration extends WidgetConfiguration
{
    public BrandingConfiguration image;
}
